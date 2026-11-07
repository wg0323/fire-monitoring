package com.fm.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.fm.common.context.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * MyBatis-Plus 插件：租户隔离、分页、审计字段填充。
 */
@Configuration
public class MybatisPlusConfig {

    /** 无 tenant_id 或平台级资产表，不参与自动租户过滤 */
    private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
            "sys_org", "sys_role_menu", "sys_user_role",
            "sys_tenant_call_config", "log_login", "log_oper",
            // 水电实时/历史表无 tenant_id 列，插件无法追加租户条件；隔离由 MonitorServiceImpl
            // 按可见设备 SN 白名单强制收口，禁止在这些表的查询里省略 SN 条件
            "b_electric_realtime", "b_electric_history", "b_water_realtime", "b_water_history",
            // 行政区划是全国统一数据，保持平台全局；其余 dict_ 表按租户隔离
            "dict_adcode"
    ));

    /** 带 merchant_id 的业务表：商户账号按 merchant_id 行级过滤 */
    private static final Set<String> MERCHANT_TABLES = new HashSet<>(Arrays.asList(
            "b_device", "b_merchant_building", "b_merchant_floor", "b_fire_alarm", "b_danger"
    ));

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContext.getTenantId());
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                if (TenantContext.isIgnoreTenant()) {
                    return true;
                }
                String name = tableName.toLowerCase();
                return IGNORE_TABLES.contains(name);
            }
        }));
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new MultiDataPermissionHandler() {
            @Override
            public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
                if (!"MERCHANT".equals(TenantContext.getUserType())) {
                    return null;
                }
                String name = table.getName().replace("`", "").toLowerCase();
                if (!MERCHANT_TABLES.contains(name)) {
                    return null;
                }
                Long merchantId = TenantContext.getMerchantId();
                if (merchantId == null) {
                    // 商户账号未绑定商户时不允许看到任何商户数据
                    return new EqualsTo(new Column("merchant_id"), new NullValue());
                }
                return new EqualsTo(new Column("merchant_id"), new LongValue(merchantId));
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
