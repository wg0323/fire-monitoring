package com.fm;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Collections;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 架构约束：Controller 文档注解、依赖注入方式与 Controller 依赖边界。
 */
class ArchitectureTest {

    /** P2 改基础包时只需调整这一行 */
    private static final String BASE_PACKAGE = "com.fm";

    private static final String PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable";
    private static final String REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam";
    private static final String REQUEST_HEADER = "org.springframework.web.bind.annotation.RequestHeader";
    private static final String PARAMETER = Parameter.class.getName();
    private static final String LAMBDA_QUERY_WRAPPER =
            "com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper";

    /**
     * 存量冻结白名单：FreezingArchRule 的等价手工实现，避免在项目里生成 archunit 允许文件。
     *
     * <p>用法：新规则首次接入若命中大量存量类，把这些类的全限定名登记到本集合，规则对它们豁免，
     * 使规则可以立即接入而不被历史债务淹没（首次接入不满屏告警）。白名单只允许随存量治理逐步
     * 删除、不允许新增；任何新写的违规类因不在名单内，会立刻让对应规则 check 失败，
     * 因此规则是「能真实失败」的，不是空跑。等价于 FreezingArchRule.freeze(rule) 的冻结效果。</p>
     *
     * <p>本批次已把 MpDeviceController 的 Mapper / LambdaQueryWrapper 越层下沉到 MerchantService，
     * 并全量补齐 VO 字段 @Schema，故 4 条新规则当前无存量违规，集合保持为空；
     * 后续如发现存量违规类，登记其全限定名即可，切勿把新代码加进来。</p>
     */
    private static final Set<String> FROZEN_LEGACY_CLASSES = Collections.emptySet();

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    void basePackageMustBeImported() {
        assertTrue(classes.size() > 0,
                "未从 " + BASE_PACKAGE + " 导入任何类，基础包名可能写错，后续规则会空跑");
    }

    @Test
    void controllersMustHaveTag() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().beAnnotatedWith(Tag.class)
                .because("每个 Controller 必须有 @Tag 才能进 API 文档");
        rule.check(classes);
    }

    @Test
    void mappingMethodsMustHaveOperationAndApiResponse() {
        ArchRule rule = methods()
                .that(mappingMethods())
                .and().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .should().beAnnotatedWith(Operation.class)
                .andShould().beAnnotatedWith(ApiResponse.class)
                .because("每个映射方法必须有 @Operation 与 @ApiResponse");
        rule.check(classes);
    }

    @Test
    void noFieldInjection() {
        ArchRule rule = fields()
                .should().notBeAnnotatedWith(Autowired.class)
                .because("依赖注入统一使用构造器注入，禁止字段注入");
        rule.check(classes);
    }

    @Test
    void controllersMustNotDependOnServiceImpl() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("ServiceImpl")
                .because("Controller 只能依赖 Service 接口");
        rule.check(classes);
    }

    @Test
    void controllersMustNotDependOnMapper() {
        ArchRule rule = noClasses()
                .that(controllersNotFrozen())
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
                .because("Controller 直接注入 Mapper 会把 SQL 构造、租户过滤与分页截断泄漏到 HTTP 层，"
                        + "应由 Service 接口封装数据访问");
        rule.check(classes);
    }

    @Test
    void controllersMustNotUseLambdaQueryWrapper() {
        ArchRule rule = noClasses()
                .that(controllersNotFrozen())
                .should().dependOnClassesThat().haveFullyQualifiedName(LAMBDA_QUERY_WRAPPER)
                .because("Controller 出现 LambdaQueryWrapper 即代表查询条件拼装越层，"
                        + "业务逻辑必须下沉到 Service，Controller 只做参数绑定与响应封装");
        rule.check(classes);
    }

    @Test
    void voFieldsMustHaveSchema() {
        ArchRule rule = fields()
                .that(voInstanceFields())
                .should().beAnnotatedWith(Schema.class)
                .because("响应 VO 的每个字段都要有 @Schema(description)，否则在线接口文档缺字段说明，"
                        + "前端无法确认字段含义与嵌套/集合元素结构");
        rule.check(classes);
    }

    @Test
    void pathAndQueryParamsMustHaveParameter() {
        ArchRule rule = methods()
                .that(mappingMethods())
                .and().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .should(haveDocumentedParameters())
                .because("路径变量/查询参数/请求头缺 @Parameter 时，在线文档无法描述该入参的名称与含义");
        rule.check(classes);
    }

    private static DescribedPredicate<JavaMethod> mappingMethods() {
        return new DescribedPredicate<JavaMethod>("映射方法") {
            @Override
            public boolean test(JavaMethod method) {
                return method.isAnnotatedWith(RequestMapping.class)
                        || method.isAnnotatedWith(GetMapping.class)
                        || method.isAnnotatedWith(PostMapping.class)
                        || method.isAnnotatedWith(PutMapping.class)
                        || method.isAnnotatedWith(DeleteMapping.class)
                        || method.isAnnotatedWith(PatchMapping.class);
            }
        };
    }

    /** 排除存量冻结类，保证白名单之外的 Controller 违规会真实失败。 */
    private static DescribedPredicate<JavaClass> controllersNotFrozen() {
        return new DescribedPredicate<JavaClass>("Controller（排除存量冻结）") {
            @Override
            public boolean test(JavaClass clazz) {
                return clazz.getSimpleName().endsWith("Controller")
                        && !FROZEN_LEGACY_CLASSES.contains(clazz.getName());
            }
        };
    }

    /** 只约束 VO 的实例字段；静态常量与存量冻结类不纳入。 */
    private static DescribedPredicate<JavaField> voInstanceFields() {
        return new DescribedPredicate<JavaField>("VO 实例字段（排除存量冻结）") {
            @Override
            public boolean test(JavaField field) {
                JavaClass owner = field.getOwner();
                return owner.getPackageName().endsWith(".vo")
                        && !field.getModifiers().contains(JavaModifier.STATIC)
                        && !FROZEN_LEGACY_CLASSES.contains(owner.getName());
            }
        };
    }

    /**
     * 逐参数校验：只有被 @PathVariable/@RequestParam/@RequestHeader 标注的参数才要求 @Parameter，
     * 请求体与响应体不加，避免对 @RequestBody / 复杂查询 DTO 误报。
     */
    private static ArchCondition<JavaMethod> haveDocumentedParameters() {
        return new ArchCondition<JavaMethod>("每个路径/查询/请求头参数都带 @Parameter") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaParameter parameter : method.getParameters()) {
                    boolean needsDoc = parameter.isAnnotatedWith(PATH_VARIABLE)
                            || parameter.isAnnotatedWith(REQUEST_PARAM)
                            || parameter.isAnnotatedWith(REQUEST_HEADER);
                    if (needsDoc && !parameter.isAnnotatedWith(PARAMETER)) {
                        events.add(SimpleConditionEvent.violated(method,
                                method.getFullName() + " 的参数缺少 @Parameter"));
                    }
                }
            }
        };
    }
}
