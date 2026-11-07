package com.fm.module.inspection.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.danger.dto.DangerSaveDTO;
import com.fm.module.danger.service.DangerService;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.inspection.dto.PlanQueryDTO;
import com.fm.module.inspection.dto.PlanResultSaveDTO;
import com.fm.module.inspection.dto.PlanSaveDTO;
import com.fm.module.inspection.entity.Plan;
import com.fm.module.inspection.entity.PlanResult;
import com.fm.module.inspection.mapper.PlanMapper;
import com.fm.module.inspection.mapper.PlanMerchantMapper;
import com.fm.module.inspection.mapper.PlanResultMapper;
import com.fm.module.inspection.vo.PlanMerchantVO;
import com.fm.module.inspection.vo.PlanVO;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.DataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 巡检测试：工作区必填、范围过滤、商户列表与隐患联动。
 */
class PlanServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Plan.class);
        TableInfoHelper.initTableInfo(assistant, PlanResult.class);
    }

    private final PlanMapper planMapper = mock(PlanMapper.class);
    private final PlanResultMapper planResultMapper = mock(PlanResultMapper.class);
    private final PlanMerchantMapper planMerchantMapper = mock(PlanMerchantMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysWorkAreaMapper workAreaMapper = mock(SysWorkAreaMapper.class);
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper = mock(SysWorkAreaMerchantMapper.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final DangerService dangerService = mock(DangerService.class);
    private TestService service;

    @BeforeEach
    void setUp() {
        service = new TestService(planResultMapper, planMerchantMapper, merchantMapper, userMapper, workAreaMapper,
                workAreaMerchantMapper, dataScopeService, dangerService);
        ReflectionTestUtils.setField(service, "baseMapper", planMapper);
        TenantContext.set(1L, "TENANT", SysRole.SCOPE_TENANT, false);
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(dataScopeService.tenantId()).thenReturn(1L);
        when(dataScopeService.canAccess(anyLong())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createRequiresWorkArea() {
        PlanSaveDTO request = planRequest(null, LocalDate.now(), LocalDate.now().plusDays(3));

        BizException ex = assertThrows(BizException.class, () -> service.create(request));

        assertEquals("请选择工作区", ex.getMessage());
    }

    @Test
    void createRejectsPastStartDate() {
        PlanSaveDTO request = planRequest(3L, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3));

        BizException ex = assertThrows(BizException.class, () -> service.create(request));

        assertEquals("开始日期不能早于今天", ex.getMessage());
    }

    @Test
    void createSavesWorkAreaAndTenant() {
        when(workAreaMapper.selectById(3L)).thenReturn(workArea(3L, 1L, "城东工作区"));
        when(planMapper.insert(any(Plan.class))).thenAnswer(invocation -> {
            PlanHolder.capture(invocation.getArgument(0));
            return 1;
        });

        service.create(planRequest(3L, LocalDate.now(), LocalDate.now().plusDays(3)));

        Plan saved = PlanHolder.captured();
        assertEquals(3L, saved.getWorkAreaId());
        assertEquals(1L, saved.getTenantId());
        assertEquals(100L, saved.getCreateBy());
    }

    @Test
    void deleteRejectsStartedPlan() {
        Plan plan = plan();
        plan.setStartDate(LocalDate.now().minusDays(1));
        when(planMapper.selectById(7L)).thenReturn(plan);

        BizException ex = assertThrows(BizException.class, () -> service.delete(7L));

        assertEquals("只允许删除未开始的计划", ex.getMessage());
        verify(planMapper, never()).deleteById(anyLong());
    }

    @Test
    void pageForFieldWithoutWorkAreaReturnsEmpty() {
        TenantContext.set(1L, "TENANT", SysRole.SCOPE_WORK_AREA, false);
        when(dataScopeService.workAreaIds(100L)).thenReturn(Collections.emptyList());

        PlanQueryDTO query = new PlanQueryDTO();
        assertTrue(service.page(query).getRecords().isEmpty());
        verify(planMapper, never()).selectPage(any(), any());
    }

    @Test
    void merchantsOnlyContainsWorkAreaMerchants() {
        when(planMapper.selectById(7L)).thenReturn(plan());
        when(workAreaMerchantMapper.selectList(any())).thenReturn(Arrays.asList(
                workAreaMerchant(10L), workAreaMerchant(20L)));
        when(merchantMapper.selectBatchIds(any())).thenReturn(Arrays.asList(
                merchant(10L, "商户甲"), merchant(20L, "商户乙")));
        PlanResult done = result(10L, PlanResult.RESULT_DANGER);
        when(planResultMapper.selectList(any())).thenReturn(Collections.singletonList(done));

        List<PlanMerchantVO> merchants = service.merchants(7L);

        assertEquals(2, merchants.size());
        PlanMerchantVO first = merchants.stream().filter(item -> item.getId().equals(10L))
                .findFirst().orElseThrow(IllegalStateException::new);
        PlanMerchantVO second = merchants.stream().filter(item -> item.getId().equals(20L))
                .findFirst().orElseThrow(IllegalStateException::new);
        assertTrue(first.getFinished());
        assertEquals(PlanResult.RESULT_DANGER, first.getLastResult());
        assertEquals("李外勤", first.getLastPatrolBy());
        assertFalse(second.getFinished());
    }

    @Test
    void saveResultRejectsMerchantOutsideWorkArea() {
        when(planMapper.selectById(7L)).thenReturn(plan());
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, "商户甲"));
        when(workAreaMerchantMapper.selectCount(any())).thenReturn(0L);

        BizException ex = assertThrows(BizException.class,
                () -> service.saveResult(resultRequest(PlanResult.RESULT_NORMAL), 100L));

        assertEquals("商户不属于该计划工作区", ex.getMessage());
    }

    @Test
    void saveResultRequiresDangerFields() {
        when(planMapper.selectById(7L)).thenReturn(plan());
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, "商户甲"));
        when(workAreaMerchantMapper.selectCount(any())).thenReturn(1L);
        PlanResultSaveDTO request = resultRequest(PlanResult.RESULT_DANGER);
        request.setDangerType(null);

        BizException ex = assertThrows(BizException.class, () -> service.saveResult(request, 100L));

        assertEquals("发现隐患时必须填写隐患类型与描述", ex.getMessage());
    }

    @Test
    void saveResultDangerCreatesDangerAndLinks() {
        when(planMapper.selectById(7L)).thenReturn(plan());
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, "商户甲"));
        when(workAreaMerchantMapper.selectCount(any())).thenReturn(1L);
        when(userMapper.selectById(100L)).thenReturn(user());
        when(dangerService.reportInspection(any(DangerSaveDTO.class))).thenReturn(999L);
        when(planResultMapper.insert(any(PlanResult.class))).thenAnswer(invocation -> {
            ResultHolder.capture(invocation.getArgument(0));
            return 1;
        });
        PlanResultSaveDTO request = resultRequest(PlanResult.RESULT_DANGER);
        request.setDangerType("消防设施");
        request.setRemark("灭火器过期");

        service.saveResult(request, 100L);

        PlanResult saved = ResultHolder.captured();
        assertEquals(999L, saved.getDangerId());
        assertEquals(3L, saved.getWorkAreaId());
        assertEquals(100L, saved.getUserId());
        assertEquals(PlanResult.RESULT_DANGER, saved.getResult());
        assertEquals("李外勤", saved.getPatrolBy());
    }

    @Test
    void saveResultNormalDoesNotCreateDanger() {
        when(planMapper.selectById(7L)).thenReturn(plan());
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, "商户甲"));
        when(workAreaMerchantMapper.selectCount(any())).thenReturn(1L);
        when(userMapper.selectById(100L)).thenReturn(user());
        when(planResultMapper.insert(any(PlanResult.class))).thenAnswer(invocation -> {
            ResultHolder.capture(invocation.getArgument(0));
            return 1;
        });

        service.saveResult(resultRequest(PlanResult.RESULT_NORMAL), 100L);

        assertNotNull(ResultHolder.captured());
        assertEquals(null, ResultHolder.captured().getDangerId());
        verify(dangerService, never()).reportInspection(any());
    }

    @Test
    void detailDerivesRunningStatus() {
        Plan plan = plan();
        plan.setStartDate(LocalDate.now().minusDays(1));
        plan.setEndDate(LocalDate.now().plusDays(1));
        when(planMapper.selectById(7L)).thenReturn(plan);
        when(workAreaMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                workArea(3L, 1L, "城东工作区")));
        when(userMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(user()));
        when(workAreaMerchantMapper.selectList(any())).thenReturn(
                Collections.singletonList(workAreaMerchant(10L)));
        when(merchantMapper.selectCount(any())).thenReturn(5L);
        when(planResultMapper.countFinished(7L)).thenReturn(2L);
        when(planResultMapper.countDanger(7L)).thenReturn(1L);

        PlanVO vo = service.detail(7L);

        assertEquals(1, vo.getStatus());
        assertEquals("城东工作区", vo.getWorkAreaName());
        assertEquals(5L, vo.getMerchantTotal());
        assertEquals(2L, vo.getFinishCount());
    }

    @Test
    void countOngoingUnfinishedCountsPlanWithRemainingMerchants() {
        when(planMapper.selectList(any())).thenReturn(Collections.singletonList(plan()));
        when(workAreaMerchantMapper.selectList(any()))
                .thenReturn(Arrays.asList(workAreaMerchant(10L), workAreaMerchant(11L)));
        when(merchantMapper.selectCount(any())).thenReturn(2L);
        when(planResultMapper.countFinished(7L)).thenReturn(1L);

        assertEquals(1L, service.countOngoingUnfinished());
    }

    @Test
    void countOngoingUnfinishedSkipsFinishedPlan() {
        when(planMapper.selectList(any())).thenReturn(Collections.singletonList(plan()));
        when(workAreaMerchantMapper.selectList(any()))
                .thenReturn(Arrays.asList(workAreaMerchant(10L), workAreaMerchant(11L)));
        when(merchantMapper.selectCount(any())).thenReturn(2L);
        when(planResultMapper.countFinished(7L)).thenReturn(2L);

        assertEquals(0L, service.countOngoingUnfinished());
    }

    @Test
    void countOngoingUnfinishedSkipsMerchantAccount() {
        TenantContext.set(1L, "MERCHANT", SysRole.SCOPE_MERCHANT, false);

        assertEquals(0L, service.countOngoingUnfinished());
        verify(planMapper, never()).selectList(any());
    }

    private PlanSaveDTO planRequest(Long workAreaId, LocalDate start, LocalDate end) {
        PlanSaveDTO request = new PlanSaveDTO();
        request.setWorkAreaId(workAreaId);
        request.setName("春季巡查");
        request.setStartDate(start);
        request.setEndDate(end);
        return request;
    }

    private PlanResultSaveDTO resultRequest(int result) {
        PlanResultSaveDTO request = new PlanResultSaveDTO();
        request.setPlanId(7L);
        request.setMerchantId(10L);
        request.setResult(result);
        request.setRemark("现场检查");
        return request;
    }

    private Plan plan() {
        Plan plan = new Plan();
        plan.setId(7L);
        plan.setTenantId(1L);
        plan.setWorkAreaId(3L);
        plan.setName("春季巡查");
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusDays(3));
        plan.setCreateBy(100L);
        plan.setCreateTime(LocalDateTime.now());
        return plan;
    }

    private SysWorkArea workArea(Long id, Long tenantId, String name) {
        SysWorkArea workArea = new SysWorkArea();
        workArea.setId(id);
        workArea.setTenantId(tenantId);
        workArea.setName(name);
        return workArea;
    }

    private SysWorkAreaMerchant workAreaMerchant(Long merchantId) {
        SysWorkAreaMerchant relation = new SysWorkAreaMerchant();
        relation.setId(merchantId);
        relation.setWorkAreaId(3L);
        relation.setMerchantId(merchantId);
        return relation;
    }

    private Merchant merchant(Long id, String name) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setTenantId(1L);
        merchant.setName(name);
        merchant.setAddr("人民路 1 号");
        merchant.setStatus(1);
        return merchant;
    }

    private PlanResult result(Long merchantId, int result) {
        PlanResult item = new PlanResult();
        item.setId(30L);
        item.setPlanId(7L);
        item.setMerchantId(merchantId);
        item.setResult(result);
        item.setPatrolBy("李外勤");
        item.setPatrolTime(LocalDateTime.now());
        return item;
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(100L);
        user.setTenantId(1L);
        user.setUserName("李外勤");
        user.setRealName("李外勤");
        user.setStatus(1);
        return user;
    }

    /**
     * 固定当前登录人，避免依赖 Sa-Token 请求上下文。
     */
    private static final class TestService extends PlanServiceImpl {

        private boolean admin = true;

        private TestService(PlanResultMapper planResultMapper, PlanMerchantMapper planMerchantMapper,
                            MerchantMapper merchantMapper, SysUserMapper userMapper,
                            SysWorkAreaMapper workAreaMapper,
                            SysWorkAreaMerchantMapper workAreaMerchantMapper,
                            DataScopeService dataScopeService, DangerService dangerService) {
            super(planResultMapper, planMerchantMapper, merchantMapper, userMapper, workAreaMapper,
                    workAreaMerchantMapper, dataScopeService, dangerService);
        }

        @Override
        protected Long currentUserId() {
            return 100L;
        }

        @Override
        protected boolean currentUserIsTenantAdmin() {
            return admin;
        }
    }

    /**
     * 保存最近一次插入的计划与结果，供断言使用。
     */
    private static final class PlanHolder {

        private static Plan value;

        private static void capture(Plan plan) {
            value = plan;
        }

        private static Plan captured() {
            return value;
        }
    }

    private static final class ResultHolder {

        private static PlanResult value;

        private static void capture(PlanResult result) {
            value = result;
        }

        private static PlanResult captured() {
            return value;
        }
    }
}
