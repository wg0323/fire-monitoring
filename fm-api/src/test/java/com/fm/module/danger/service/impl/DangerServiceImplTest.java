package com.fm.module.danger.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.danger.dto.DangerAssignDTO;
import com.fm.module.danger.dto.DangerAuditDTO;
import com.fm.module.danger.dto.DangerBatchAssignDTO;
import com.fm.module.danger.dto.DangerDealDTO;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.entity.DangerResult;
import com.fm.module.danger.entity.DangerTrace;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.danger.mapper.DangerResultMapper;
import com.fm.module.danger.mapper.DangerTraceMapper;
import com.fm.module.danger.vo.DangerBatchAssignResultVO;
import com.fm.module.danger.vo.DangerTimelineVO;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.notification.service.MpNotificationService;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.DataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 隐患闭环测试：状态机、责任人归属、审核留痕与时间线节点。
 */
class DangerServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Danger.class);
        TableInfoHelper.initTableInfo(assistant, DangerResult.class);
        TableInfoHelper.initTableInfo(assistant, DangerTrace.class);
    }

    private final DangerMapper dangerMapper = mock(DangerMapper.class);
    private final DangerResultMapper resultMapper = mock(DangerResultMapper.class);
    private final DangerTraceMapper traceMapper = mock(DangerTraceMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final SysWorkAreaMapper workAreaMapper = mock(SysWorkAreaMapper.class);
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper = mock(SysWorkAreaMerchantMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysUserWorkAreaMapper userWorkAreaMapper = mock(SysUserWorkAreaMapper.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final MpNotificationService notificationService = mock(MpNotificationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private TestService service;

    @BeforeEach
    void setUp() {
        service = new TestService(dangerMapper, resultMapper, traceMapper, merchantMapper, workAreaMapper,
                workAreaMerchantMapper, userMapper, userRoleMapper, roleMapper, userWorkAreaMapper,
                dataScopeService, notificationService, objectMapper, transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        TenantContext.set(1L, "TENANT", SysRole.SCOPE_TENANT, false);
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(dataScopeService.canAccess(anyLong())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void assignRejectsNonPendingDanger() {
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_DEAL));

        BizException ex = assertThrows(BizException.class, () -> service.assign(1L, assignRequest(200L)));

        assertEquals("该隐患不是待指派状态", ex.getMessage());
        verify(dangerMapper, never()).update(isNull(), any());
    }

    @Test
    void batchAssignAssignsAllPendingDangers() {
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_ASSIGN));
        Danger second = danger(Danger.STATUS_PENDING_ASSIGN);
        second.setId(2L);
        when(dangerMapper.selectById(2L)).thenReturn(second);
        when(userMapper.selectById(200L)).thenReturn(user(200L, "李外勤", 1L));
        when(roleMapper.selectOne(any())).thenReturn(fieldRole());
        when(userRoleMapper.selectCount(any())).thenReturn(1L);
        when(userWorkAreaMapper.selectCount(any())).thenReturn(1L);
        when(dangerMapper.update(isNull(), any())).thenReturn(1);

        DangerBatchAssignResultVO result = service.batchAssign(batchRequest(Arrays.asList(1L, 2L)));

        assertEquals(2, result.getTotal());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
        verify(traceMapper, times(2)).insert(any(DangerTrace.class));
        verify(notificationService, times(1)).sendDanger(any(Danger.class), any(), anyString());
    }

    @Test
    void batchAssignKeepsGoingWhenOneDangerAlreadyAssigned() {
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_ASSIGN));
        when(dangerMapper.selectById(2L)).thenReturn(danger(Danger.STATUS_PENDING_DEAL));
        when(userMapper.selectById(200L)).thenReturn(user(200L, "李外勤", 1L));
        when(roleMapper.selectOne(any())).thenReturn(fieldRole());
        when(userRoleMapper.selectCount(any())).thenReturn(1L);
        when(userWorkAreaMapper.selectCount(any())).thenReturn(1L);
        when(dangerMapper.update(isNull(), any())).thenReturn(1);

        DangerBatchAssignResultVO result = service.batchAssign(batchRequest(Arrays.asList(1L, 2L)));

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals("该隐患不是待指派状态", result.getFailures().get(0).getReason());
    }

    @Test
    void batchAssignRejectsMoreThanMaxSize() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1L; i <= 101L; i++) {
            ids.add(i);
        }

        BizException ex = assertThrows(BizException.class, () -> service.batchAssign(batchRequest(ids)));

        assertEquals("单次最多指派 100 条隐患", ex.getMessage());
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void assignRejectsUserWithoutFieldRole() {
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_ASSIGN));
        when(userMapper.selectById(200L)).thenReturn(user(200L, "李外勤", 1L));
        when(roleMapper.selectOne(any())).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> service.assign(1L, assignRequest(200L)));

        assertEquals("所选人员不是外勤", ex.getMessage());
    }

    @Test
    void assignRejectsUserOutsideWorkArea() {
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_ASSIGN));
        when(userMapper.selectById(200L)).thenReturn(user(200L, "李外勤", 1L));
        when(roleMapper.selectOne(any())).thenReturn(fieldRole());
        when(userRoleMapper.selectCount(any())).thenReturn(1L);
        when(userWorkAreaMapper.selectCount(any())).thenReturn(0L);

        BizException ex = assertThrows(BizException.class, () -> service.assign(1L, assignRequest(200L)));

        assertEquals("所选人员不属于该隐患工作区", ex.getMessage());
    }

    @Test
    void assignMovesDangerToPendingDeal() {
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_ASSIGN));
        when(userMapper.selectById(200L)).thenReturn(user(200L, "李外勤", 1L));
        when(roleMapper.selectOne(any())).thenReturn(fieldRole());
        when(userRoleMapper.selectCount(any())).thenReturn(1L);
        when(userWorkAreaMapper.selectCount(any())).thenReturn(1L);
        when(dangerMapper.update(isNull(), any())).thenReturn(1);

        service.assign(1L, assignRequest(200L));

        LambdaUpdateWrapper<Danger> wrapper = capturedUpdateWrapper();
        String sqlSet = wrapper.getSqlSet();
        assertTrue(sqlSet.contains("status"));
        assertTrue(sqlSet.contains("assignee_id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Danger.STATUS_PENDING_DEAL));
        verify(traceMapper).insert(any(DangerTrace.class));
        verify(notificationService).sendDanger(any(Danger.class), any(), any());
    }

    @Test
    void dealRejectsFieldUserNotAssignee() {
        TenantContext.set(1L, "TENANT", SysRole.SCOPE_WORK_AREA, false);
        Danger danger = danger(Danger.STATUS_PENDING_DEAL);
        danger.setAssigneeType(Danger.ASSIGNEE_FIELD);
        danger.setAssigneeId(200L);
        when(dangerMapper.selectById(1L)).thenReturn(danger);

        BizException ex = assertThrows(BizException.class, () -> service.deal(1L, dealRequest()));

        assertEquals("隐患不存在", ex.getMessage());
    }

    @Test
    void dealRejectsMerchantOfOtherStore() {
        TenantContext.set(1L, null, 10L, "MERCHANT", SysRole.SCOPE_MERCHANT, false);
        Danger danger = danger(Danger.STATUS_PENDING_DEAL);
        danger.setMerchantId(11L);
        danger.setAssigneeType(Danger.ASSIGNEE_MERCHANT);
        when(dangerMapper.selectById(1L)).thenReturn(danger);

        BizException ex = assertThrows(BizException.class, () -> service.deal(1L, dealRequest()));

        assertEquals("隐患不存在", ex.getMessage());
    }

    @Test
    void dealMovesDangerToPendingAuditAndKeepsResult() {
        Danger danger = danger(Danger.STATUS_PENDING_DEAL);
        danger.setAssigneeType(Danger.ASSIGNEE_MERCHANT);
        danger.setMerchantId(10L);
        when(dangerMapper.selectById(1L)).thenReturn(danger);
        when(dangerMapper.update(isNull(), any())).thenReturn(1);
        TenantContext.set(1L, null, 10L, "MERCHANT", SysRole.SCOPE_MERCHANT, false);

        service.deal(1L, dealRequest());

        LambdaUpdateWrapper<Danger> wrapper = capturedUpdateWrapper();
        assertTrue(wrapper.getSqlSet().contains("status"));
        verify(resultMapper).insert(any(DangerResult.class));
        verify(traceMapper).insert(any(DangerTrace.class));
    }

    @Test
    void dealClearsPreviousAuditFields() {
        Danger danger = danger(Danger.STATUS_PENDING_DEAL);
        danger.setAssigneeType(Danger.ASSIGNEE_MERCHANT);
        danger.setMerchantId(10L);
        when(dangerMapper.selectById(1L)).thenReturn(danger);
        when(dangerMapper.update(isNull(), any())).thenReturn(1);
        TenantContext.set(1L, null, 10L, "MERCHANT", SysRole.SCOPE_MERCHANT, false);

        service.deal(1L, dealRequest());

        assertTrue(capturedUpdateWrapper().getSqlSet().contains("audit_time"));
    }

    @Test
    void reportRejectsMerchantOutsideWorkArea() {
        TenantContext.set(1L, "TENANT", SysRole.SCOPE_WORK_AREA, false);
        when(merchantMapper.selectById(10L)).thenReturn(merchant());
        when(dataScopeService.workAreaMerchantIds(100L)).thenReturn(Collections.singletonList(20L));

        BizException ex = assertThrows(BizException.class, () -> service.report(reportRequest()));

        assertEquals(403, ex.getCode());
        assertEquals("只能上报所属工作区的商户", ex.getMessage());
    }

    @Test
    void auditRejectsNonOperator() {
        service.admin = false;
        service.duty = false;

        BizException ex = assertThrows(BizException.class, () -> service.audit(1L, auditRequest(1, null)));

        assertEquals(403, ex.getCode());
        assertEquals("仅租户管理员或值守可审核隐患", ex.getMessage());
    }

    @Test
    void auditAllowsDuty() {
        service.admin = false;
        service.duty = true;
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_AUDIT));
        when(dangerMapper.update(isNull(), any())).thenReturn(1);
        when(resultMapper.selectOne(any())).thenReturn(dealResult());

        service.audit(1L, auditRequest(1, "整改到位"));

        assertTrue(capturedUpdateWrapper().getSqlSet().contains("audit_by_name"));
    }

    @Test
    void auditRejectRequiresRemark() {
        service.admin = true;
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_AUDIT));

        BizException ex = assertThrows(BizException.class, () -> service.audit(1L, auditRequest(4, null)));

        assertEquals("驳回必须填写原因", ex.getMessage());
    }

    @Test
    void auditPassCompletesDanger() {
        service.admin = true;
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_AUDIT));
        when(dangerMapper.update(isNull(), any())).thenReturn(1);
        when(resultMapper.selectOne(any())).thenReturn(dealResult());

        service.audit(1L, auditRequest(1, "整改到位"));

        LambdaUpdateWrapper<Danger> wrapper = capturedUpdateWrapper();
        assertTrue(wrapper.getSqlSet().contains("audit_result"));
        assertTrue(wrapper.getSqlSet().contains("audit_by_name"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Danger.STATUS_DONE));
        verify(resultMapper).update(isNull(), any());
        verify(traceMapper).insert(any(DangerTrace.class));
        verify(notificationService).sendDanger(any(Danger.class), any(), any());
    }

    @Test
    void auditRejectReturnsToPendingDealAndKeepsHistory() {
        service.admin = true;
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_AUDIT));
        when(dangerMapper.update(isNull(), any())).thenReturn(1);
        when(resultMapper.selectOne(any())).thenReturn(dealResult());

        service.audit(1L, auditRequest(4, "照片不清晰"));

        LambdaUpdateWrapper<Danger> wrapper = capturedUpdateWrapper();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Danger.STATUS_PENDING_DEAL));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(DangerAuditDTO.RESULT_REJECT));
        assertEquals(1, wrapper.getParamNameValuePairs().values().stream()
                .filter(value -> "照片不清晰".equals(value)).count());
        verify(resultMapper).update(isNull(), any());
    }

    @Test
    void dealRejectsCompletedDanger() {
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_DONE));

        BizException ex = assertThrows(BizException.class, () -> service.deal(1L, dealRequest()));

        assertEquals("只有待整改的隐患才能提交整改", ex.getMessage());
    }

    @Test
    void detailRejectsOtherMerchant() {
        TenantContext.set(1L, null, 10L, "MERCHANT", SysRole.SCOPE_MERCHANT, false);
        Danger danger = danger(Danger.STATUS_PENDING_DEAL);
        danger.setMerchantId(11L);
        when(dangerMapper.selectById(1L)).thenReturn(danger);

        BizException ex = assertThrows(BizException.class, () -> service.detail(1L));

        assertEquals("隐患不存在", ex.getMessage());
    }

    @Test
    void reportCreatesMerchantAssignedDanger() {
        when(merchantMapper.selectById(10L)).thenReturn(merchant());
        when(userMapper.selectOne(any())).thenReturn(user(300L, "商户张老板", 1L));
        when(workAreaMerchantMapper.selectOne(any())).thenReturn(workAreaMerchant());
        when(dangerMapper.insert(any(Danger.class))).thenAnswer(invocation -> {
            DangerousHolder.capture(invocation.getArgument(0));
            return 1;
        });

        service.report(reportRequest());

        Danger saved = DangerousHolder.captured();
        assertEquals(Danger.STATUS_PENDING_DEAL, saved.getStatus());
        assertEquals(Danger.SOURCE_SNAPSHOT, saved.getSourceType());
        assertEquals(Danger.ASSIGNEE_MERCHANT, saved.getAssigneeType());
        assertEquals(3L, saved.getWorkAreaId());
        assertEquals(300L, saved.getAssigneeId());
        verify(traceMapper).insert(any(DangerTrace.class));
        verify(notificationService).sendDanger(any(Danger.class), any(), any());
    }

    @Test
    void reportInspectionUsesInspectionSource() {
        when(merchantMapper.selectById(10L)).thenReturn(merchant());
        when(userMapper.selectOne(any())).thenReturn(user(300L, "商户张老板", 1L));
        when(workAreaMerchantMapper.selectOne(any())).thenReturn(workAreaMerchant());
        when(dangerMapper.insert(any(Danger.class))).thenAnswer(invocation -> {
            DangerousHolder.capture(invocation.getArgument(0));
            return 1;
        });

        service.reportInspection(reportRequest());

        assertEquals(Danger.SOURCE_INSPECTION, DangerousHolder.captured().getSourceType());
    }

    @Test
    void timelineReturnsFixedNodesWithMissingMarked() {
        when(dangerMapper.selectById(1L)).thenReturn(danger(Danger.STATUS_PENDING_DEAL));
        when(traceMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(resultMapper.selectOne(any())).thenReturn(null);

        List<DangerTimelineVO> nodes = service.timeline(1L);

        assertEquals(6, nodes.size());
        DangerTimelineVO report = nodes.get(0);
        assertEquals(DangerTimelineVO.NODE_REPORT, report.getNodeKey());
        assertTrue(report.getOccurred());
        DangerTimelineVO alarm = nodes.get(1);
        assertEquals(DangerTimelineVO.NODE_ALARM, alarm.getNodeKey());
        assertFalse(alarm.getOccurred());
        assertNull(alarm.getTime());
        DangerTimelineVO convert = nodes.get(2);
        assertFalse(convert.getOccurred());
        assertNull(convert.getContent());
    }

    @Test
    void timelineMarksAutoConvertSource() {
        Danger danger = danger(Danger.STATUS_PENDING_DEAL);
        danger.setConvertTime(LocalDateTime.now());
        when(dangerMapper.selectById(1L)).thenReturn(danger);
        DangerTrace trace = new DangerTrace();
        trace.setId(9L);
        trace.setTraceType(DangerTrace.TYPE_CONVERT);
        trace.setDetail("{\"convertSource\":\"AUTO\"}");
        when(traceMapper.selectList(any())).thenReturn(Collections.singletonList(trace));
        when(resultMapper.selectOne(any())).thenReturn(null);

        List<DangerTimelineVO> nodes = service.timeline(1L);

        DangerTimelineVO convert = nodes.get(2);
        assertTrue(convert.getOccurred());
        assertEquals("AUTO", convert.getExtra());
        assertTrue(convert.getContent().contains("自动转"));
    }

    private Danger danger(int status) {
        Danger danger = new Danger();
        danger.setId(1L);
        danger.setTenantId(1L);
        danger.setMerchantId(10L);
        danger.setWorkAreaId(3L);
        danger.setStatus(status);
        danger.setRemark("灭火器过期");
        danger.setPosition("一楼大厅");
        danger.setCreateTime(LocalDateTime.now().minusDays(1));
        return danger;
    }

    private DangerAssignDTO assignRequest(Long assigneeId) {
        DangerAssignDTO request = new DangerAssignDTO();
        request.setAssigneeId(assigneeId);
        return request;
    }

    private DangerBatchAssignDTO batchRequest(List<Long> ids) {
        DangerBatchAssignDTO request = new DangerBatchAssignDTO();
        request.setIds(ids);
        request.setAssigneeId(200L);
        return request;
    }

    private DangerDealDTO dealRequest() {
        DangerDealDTO request = new DangerDealDTO();
        request.setRemark("已更换灭火器");
        return request;
    }

    private DangerAuditDTO auditRequest(int result, String remark) {
        DangerAuditDTO request = new DangerAuditDTO();
        request.setAuditResult(result);
        request.setAuditRemark(remark);
        return request;
    }

    private com.fm.module.danger.dto.DangerSaveDTO reportRequest() {
        com.fm.module.danger.dto.DangerSaveDTO request =
                new com.fm.module.danger.dto.DangerSaveDTO();
        request.setMerchantId(10L);
        request.setDangerType("消防设施");
        request.setRemark("灭火器过期");
        return request;
    }

    private SysUser user(Long id, String name, Long tenantId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUserName(name);
        user.setRealName(name);
        user.setStatus(1);
        user.setUserType("TENANT");
        return user;
    }

    private SysRole fieldRole() {
        SysRole role = new SysRole();
        role.setId(9L);
        role.setRoleKey(SysRole.TENANT_FIELD);
        return role;
    }

    private Merchant merchant() {
        Merchant merchant = new Merchant();
        merchant.setId(10L);
        merchant.setTenantId(1L);
        merchant.setName("示例商户");
        merchant.setAddr("人民路 1 号");
        return merchant;
    }

    private SysWorkAreaMerchant workAreaMerchant() {
        SysWorkAreaMerchant relation = new SysWorkAreaMerchant();
        relation.setId(5L);
        relation.setWorkAreaId(3L);
        relation.setMerchantId(10L);
        return relation;
    }

    private DangerResult dealResult() {
        DangerResult result = new DangerResult();
        result.setId(5L);
        result.setDangerId(1L);
        result.setDealBy("李外勤");
        result.setDealUserName("李外勤");
        result.setDealTime(LocalDateTime.now());
        result.setRemark("已更换灭火器");
        return result;
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<Danger> capturedUpdateWrapper() {
        ArgumentCaptor<LambdaUpdateWrapper<Danger>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(dangerMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }

    /**
     * 固定当前登录人，避免依赖 Sa-Token 请求上下文。
     */
    private static final class TestService extends DangerServiceImpl {

        private boolean admin = true;
        private boolean duty = false;

        private TestService(DangerMapper dangerMapper, DangerResultMapper resultMapper,
                            DangerTraceMapper traceMapper, MerchantMapper merchantMapper,
                            SysWorkAreaMapper workAreaMapper, SysWorkAreaMerchantMapper workAreaMerchantMapper,
                            SysUserMapper userMapper, SysUserRoleMapper userRoleMapper,
                            SysRoleMapper roleMapper, SysUserWorkAreaMapper userWorkAreaMapper,
                            DataScopeService dataScopeService, MpNotificationService notificationService,
                            ObjectMapper objectMapper, TransactionTemplate transactionTemplate) {
            super(dangerMapper, resultMapper, traceMapper, merchantMapper, workAreaMapper,
                    workAreaMerchantMapper, userMapper, userRoleMapper, roleMapper, userWorkAreaMapper,
                    dataScopeService, notificationService, objectMapper, transactionTemplate);
        }

        @Override
        protected Long currentUserId() {
            return 100L;
        }

        @Override
        protected String currentUserName() {
            return "张管理";
        }

        @Override
        protected boolean currentUserIsTenantAdmin() {
            return admin;
        }

        @Override
        protected boolean currentUserHasRole(String roleKey) {
            return SysRole.TENANT_DUTY.equals(roleKey) && duty;
        }
    }

    /**
     * 保存最近一次插入的隐患，供断言使用。
     */
    private static final class DangerousHolder {

        private static Danger value;

        private static void capture(Danger danger) {
            value = danger;
        }

        private static Danger captured() {
            return value;
        }
    }
}
