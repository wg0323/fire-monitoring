package com.fm.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.dto.AlarmCallDTO;
import com.fm.module.alarm.dto.AlarmDispatchDTO;
import com.fm.module.alarm.dto.AlarmMerchantHandleDTO;
import com.fm.module.alarm.dto.AlarmQueryDTO;
import com.fm.module.alarm.dto.AlarmVerifyDTO;
import com.fm.module.alarm.entity.CallRecord;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.entity.FireAlarmTrace;
import com.fm.module.alarm.mapper.CallRecordMapper;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.mapper.FireAlarmTraceMapper;
import com.fm.module.alarm.mq.DeviceMessage;
import com.fm.module.alarm.service.AlarmDomainService;
import com.fm.module.alarm.service.AlarmService;
import com.fm.module.alarm.vo.AlarmMerchantVO;
import com.fm.module.alarm.vo.AlarmVO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictService;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.DataScopeService;
import com.fm.module.workorder.entity.WorkOrder;
import com.fm.module.workorder.mapper.WorkOrderMapper;
import com.fm.module.workorder.service.WorkOrderService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报警服务测试：消息幂等、商户处置分支与派单选人。
 */
class AlarmServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, FireAlarm.class);
        TableInfoHelper.initTableInfo(assistant, WorkOrder.class);
    }

    private final FireAlarmMapper alarmMapper = mock(FireAlarmMapper.class);
    private final FireAlarmTraceMapper traceMapper = mock(FireAlarmTraceMapper.class);
    private final CallRecordMapper callRecordMapper = mock(CallRecordMapper.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final MerchantFloorMapper floorMapper = mock(MerchantFloorMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysWorkAreaMapper workAreaMapper = mock(SysWorkAreaMapper.class);
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper = mock(SysWorkAreaMerchantMapper.class);
    private final SysUserWorkAreaMapper userWorkAreaMapper = mock(SysUserWorkAreaMapper.class);
    private final WorkOrderMapper workOrderMapper = mock(WorkOrderMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final AlarmDomainService alarmDomainService = mock(AlarmDomainService.class);
    private final WorkOrderService workOrderService = mock(WorkOrderService.class);
    private final DictService dictService = mock(DictService.class);
    private final AlarmService service = new AlarmServiceImpl(alarmMapper, traceMapper, callRecordMapper,
            deviceMapper, merchantMapper, floorMapper, userMapper, userRoleMapper, roleMapper, workAreaMapper,
            workAreaMerchantMapper, userWorkAreaMapper, workOrderMapper, objectMapper, eventPublisher,
            dataScopeService, alarmDomainService, workOrderService, dictService);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void tenDuplicateMessagesProduceSingleAlarm() throws Exception {
        Device device = new Device();
        device.setId(7L);
        device.setSn("SN-1");
        device.setMerchantId(10L);
        device.setType("SMOKE");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(alarmMapper.selectCount(any())).thenReturn(0L, 0L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L);

        DeviceMessage message = new DeviceMessage();
        message.setTenantId(1L);
        message.setMessageId("msg-1");
        message.setSn("SN-1");
        message.setType("ALARM");
        message.setEventTime(System.currentTimeMillis());
        message.setPayload(objectMapper.createObjectNode().put("alarmType", "FIRE"));
        for (int i = 0; i < 10; i++) {
            service.handleAlarm(message);
        }

        verify(alarmMapper, times(1)).insert(any(FireAlarm.class));
        verify(traceMapper, times(1)).insert(any());
    }

    @Test
    void merchantFireKeepsAlarmPendingForDutyReview() {
        TenantContext.set(1L, null, 10L, "MERCHANT", "MERCHANT", false);
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        when(alarmMapper.update(isNull(), any())).thenReturn(1);

        service.merchantHandle(handleRequest(FireAlarm.RESULT_TRUE));

        verify(alarmDomainService, never()).finalizeAlarm(any(), any(), anyString(), any(), any(),
                any(), any(), any(), any());
        verify(alarmDomainService).appendTrace(eq(alarm), eq(7), eq(1), anyString(), any());
        ArgumentCaptor<LambdaUpdateWrapper<FireAlarm>> captor = wrapperCaptor();
        verify(alarmMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("verify_status"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(FireAlarm.SOURCE_MERCHANT));
    }

    @Test
    void merchantFalseReportFinalizesImmediately() {
        TenantContext.set(1L, null, 10L, "MERCHANT", "MERCHANT", false);
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        when(alarmMapper.update(isNull(), any())).thenReturn(1);

        service.merchantHandle(handleRequest(FireAlarm.RESULT_FALSE));

        verify(alarmDomainService).finalizeAlarm(eq(5L), eq(FireAlarm.RESULT_FALSE),
                eq(FireAlarm.SOURCE_MERCHANT), isNull(), isNull(), any(), any(), any(), any());
    }

    @Test
    void merchantFaultResultRejected() {
        BizException error = assertThrows(BizException.class,
                () -> service.merchantHandle(handleRequest(FireAlarm.RESULT_FAULT)));

        assertEquals("处置结论只能是误报或火警", error.getMessage());
        verify(alarmDomainService, never()).finalizeAlarm(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void merchantHandleRejectsFaultAlarm() {
        TenantContext.set(1L, null, 10L, "MERCHANT", "MERCHANT", false);
        FireAlarm alarm = pendingAlarm();
        alarm.setAlarmLevel(3);
        when(alarmMapper.selectById(5L)).thenReturn(alarm);

        BizException error = assertThrows(BizException.class,
                () -> service.merchantHandle(handleRequest(FireAlarm.RESULT_FALSE)));

        assertEquals("设备故障报警由转隐患流程处理，无需商户处置", error.getMessage());
        verify(alarmDomainService, never()).finalizeAlarm(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void dispatchRejectsFaultAlarm() {
        FireAlarm alarm = pendingAlarm();
        alarm.setAlarmLevel(3);
        when(alarmMapper.selectById(5L)).thenReturn(alarm);

        BizException error = assertThrows(BizException.class, () -> service.dispatch(dispatchRequest()));

        assertEquals("故障报警只能转隐患，不能派单核警", error.getMessage());
    }

    @Test
    void dispatchRejectsHandlerOutsideWorkArea() {
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        SysWorkArea area = new SysWorkArea();
        area.setId(3L);
        area.setTenantId(1L);
        area.setName("城区");
        when(workAreaMapper.selectById(3L)).thenReturn(area);
        SysUser handler = new SysUser();
        handler.setId(9L);
        handler.setTenantId(1L);
        handler.setUserType("TENANT");
        handler.setStatus(1);
        when(userMapper.selectById(9L)).thenReturn(handler);
        when(userWorkAreaMapper.selectCount(any())).thenReturn(0L);

        BizException error = assertThrows(BizException.class, () -> service.dispatch(dispatchRequest()));

        assertEquals("所选外勤不属于该工作区", error.getMessage());
        verify(workOrderService, never()).createFromAlarm(any(), any(), any(), any(),
                any(), any(), any(), any(), any());
    }

    @Test
    void dispatchCreatesWorkOrderWithSelectedWorkArea() {
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        SysWorkArea area = new SysWorkArea();
        area.setId(3L);
        area.setTenantId(1L);
        area.setName("城区");
        when(workAreaMapper.selectById(3L)).thenReturn(area);
        SysUser handler = new SysUser();
        handler.setId(9L);
        handler.setTenantId(1L);
        handler.setUserType("TENANT");
        handler.setStatus(1);
        handler.setRealName("张师傅");
        when(userMapper.selectById(9L)).thenReturn(handler);
        when(userWorkAreaMapper.selectCount(any())).thenReturn(1L);
        when(roleMapper.selectOne(any())).thenReturn(null);
        when(workAreaMerchantMapper.selectCount(any())).thenReturn(1L);
        when(alarmMapper.update(isNull(), any())).thenReturn(1);

        service.dispatch(dispatchRequest());

        verify(workOrderService).createFromAlarm(eq(1L), eq(10L), eq(5L), eq(3L),
                anyString(), anyString(), any(), eq(9L), eq("张师傅"));
    }

    @Test
    void deviceFaultAlarmGetsOneHourDeadlineOnIngest() throws Exception {
        Device device = new Device();
        device.setId(7L);
        device.setSn("SN-FAULT");
        device.setMerchantId(10L);
        device.setType("SMOKE");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(alarmMapper.selectCount(any())).thenReturn(0L, 0L);

        DeviceMessage message = new DeviceMessage();
        message.setTenantId(1L);
        message.setMessageId("msg-fault");
        message.setSn("SN-FAULT");
        message.setType("ALARM");
        message.setEventTime(System.currentTimeMillis());
        message.setPayload(objectMapper.createObjectNode().put("alarmType", "FAULT"));
        service.handleAlarm(message);

        ArgumentCaptor<FireAlarm> captor = ArgumentCaptor.forClass(FireAlarm.class);
        verify(alarmMapper).insert(captor.capture());
        FireAlarm inserted = captor.getValue();
        assertEquals(3, inserted.getAlarmLevel());
        assertTrue(inserted.getDangerConvertDeadline().isEqual(inserted.getAlarmTime().plusHours(1)));
    }

    @Test
    void legacyPreAlarmPayloadIsStoredAsFire() throws Exception {
        Device device = new Device();
        device.setId(8L);
        device.setSn("SN-LEGACY");
        device.setMerchantId(10L);
        device.setType("SMOKE");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(alarmMapper.selectCount(any())).thenReturn(0L, 0L);

        DeviceMessage message = new DeviceMessage();
        message.setTenantId(1L);
        message.setMessageId("msg-legacy");
        message.setSn("SN-LEGACY");
        message.setType("ALARM");
        message.setEventTime(System.currentTimeMillis());
        message.setPayload(objectMapper.createObjectNode()
                .put("alarmType", "PRE_ALARM")
                .put("level", 2));
        service.handleAlarm(message);

        ArgumentCaptor<FireAlarm> captor = ArgumentCaptor.forClass(FireAlarm.class);
        verify(alarmMapper).insert(captor.capture());
        FireAlarm inserted = captor.getValue();
        assertEquals(1, inserted.getAlarmLevel());
        assertEquals("FIRE", inserted.getAlarmType());
    }

    @Test
    void callRejectsFaultAlarm() {
        FireAlarm alarm = pendingAlarm();
        alarm.setAlarmLevel(3);
        when(alarmMapper.selectById(5L)).thenReturn(alarm);

        BizException error = assertThrows(BizException.class, () -> service.call(callRequest(true)));

        assertEquals("故障报警只能转隐患，无需电话告警", error.getMessage());
        verify(callRecordMapper, never()).insert(any(CallRecord.class));
    }

    @Test
    void should_write_call_alarm_trace_when_phone_alert_connected() {
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);

        service.call(callRequest(true));

        verify(alarmDomainService).appendTrace(eq(alarm), eq(FireAlarmTrace.TYPE_CALL_ALARM), eq(1),
                contains("已接通"), any());
        verify(callRecordMapper).insert(any(CallRecord.class));
    }

    @Test
    void should_write_failed_call_alarm_trace_when_phone_alert_not_connected() {
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);

        service.call(callRequest(false));

        verify(alarmDomainService).appendTrace(eq(alarm), eq(FireAlarmTrace.TYPE_CALL_ALARM), eq(2),
                contains("需派单核警"), any());
    }

    @Test
    void should_update_existing_call_record_when_action_id_provided() {
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        CallRecord existing = new CallRecord();
        existing.setId(88L);
        when(callRecordMapper.selectOne(any())).thenReturn(existing);

        AlarmCallDTO request = callRequest(false);
        request.setActionId("action-1");
        service.call(request);

        verify(callRecordMapper, never()).insert(any());
        verify(callRecordMapper).updateById(any(CallRecord.class));
        verify(alarmDomainService).appendTrace(eq(alarm), eq(FireAlarmTrace.TYPE_CALL_ALARM), eq(2),
                anyString(), any());
    }

    @Test
    void should_finalize_alarm_when_verify_submitted() {
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        when(workOrderMapper.selectCount(any())).thenReturn(0L);

        service.verify(verifyRequest());

        verify(alarmDomainService).finalizeAlarm(eq(5L), eq(FireAlarm.RESULT_TRUE),
                eq(FireAlarm.SOURCE_DUTY), isNull(), isNull(), any(), any(), any(), any());
    }

    @Test
    void verifyRejectsFaultAlarm() {
        FireAlarm alarm = pendingAlarm();
        alarm.setAlarmLevel(3);
        when(alarmMapper.selectById(5L)).thenReturn(alarm);

        BizException error = assertThrows(BizException.class, () -> service.verify(verifyRequest()));

        assertEquals("设备故障报警由转隐患流程处理，无需核警", error.getMessage());
        verify(alarmDomainService, never()).finalizeAlarm(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void verifyRejectsAlarmWithActiveWorkOrder() {
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        when(workOrderMapper.selectCount(any())).thenReturn(1L);

        BizException error = assertThrows(BizException.class, () -> service.verify(verifyRequest()));

        assertEquals("该报警已派单，请由外勤完成工单后收口", error.getMessage());
        verify(alarmDomainService, never()).finalizeAlarm(any(), any(), anyString(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void dispatchRejectsAlarmAlreadyDispatchedByConcurrentRequest() {
        FireAlarm alarm = pendingAlarm();
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        SysWorkArea area = new SysWorkArea();
        area.setId(3L);
        area.setTenantId(1L);
        when(workAreaMapper.selectById(3L)).thenReturn(area);
        SysUser handler = new SysUser();
        handler.setId(9L);
        handler.setTenantId(1L);
        handler.setUserType("TENANT");
        handler.setStatus(1);
        when(userMapper.selectById(9L)).thenReturn(handler);
        when(userWorkAreaMapper.selectCount(any())).thenReturn(1L);
        when(roleMapper.selectOne(any())).thenReturn(null);
        when(workAreaMerchantMapper.selectCount(any())).thenReturn(1L);
        when(alarmMapper.update(isNull(), any())).thenReturn(0);

        BizException error = assertThrows(BizException.class, () -> service.dispatch(dispatchRequest()));

        assertEquals("该报警已处理，不能派单", error.getMessage());
        verify(workOrderService, never()).createFromAlarm(any(), any(), any(), any(),
                any(), any(), any(), any(), any());
    }

    @Test
    void should_show_device_type_name_when_alarm_only_stores_type_code() {
        FireAlarm alarm = pendingAlarm();
        alarm.setDeviceType("dh-wt2ga");
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        when(merchantMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
        when(workOrderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(dictService.deviceTypeNames(any())).thenReturn(Collections.singletonMap("dh-wt2ga", "大华感温"));

        AlarmVO vo = service.detail(5L);

        assertEquals("大华感温", vo.getDeviceTypeName());
    }

    @Test
    void should_show_device_type_name_on_alarm_page_when_rows_have_type_code() {
        FireAlarm alarm = pendingAlarm();
        alarm.setDeviceType("dh-sa2ga-a");
        Page<FireAlarm> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(alarm));
        doReturn(page).when(alarmMapper).selectPage(any(), any());
        when(merchantMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
        when(workOrderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(dictService.deviceTypeNames(any())).thenReturn(Collections.singletonMap("dh-sa2ga-a", "大华感烟"));

        Page<AlarmVO> voPage = service.pageVO(new AlarmQueryDTO());

        assertEquals("大华感烟", voPage.getRecords().get(0).getDeviceTypeName());
    }

    @Test
    void should_exclude_verified_alarms_when_query_sets_exclude_verified_true() {
        AlarmQueryDTO query = new AlarmQueryDTO();
        query.setExcludeVerified(true);
        doReturn(new Page<FireAlarm>(1, 10, 0)).when(alarmMapper).selectPage(any(), any());

        service.pageVO(query);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<FireAlarm>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(alarmMapper).selectPage(any(), wrapperCaptor.capture());
        String sql = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sql.contains("verify_status"));
        assertTrue(sql.contains("<>"));
    }

    @Test
    void should_keep_verified_alarms_when_query_does_not_set_exclude_verified() {
        doReturn(new Page<FireAlarm>(1, 10, 0)).when(alarmMapper).selectPage(any(), any());

        service.pageVO(new AlarmQueryDTO());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<FireAlarm>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(alarmMapper).selectPage(any(), wrapperCaptor.capture());
        assertFalse(wrapperCaptor.getValue().getSqlSegment().contains("verify_status"));
    }

    @Test
    void should_only_return_merchants_with_unverified_alarms_when_unverified_only_true() {
        AlarmMerchantVO stat = new AlarmMerchantVO();
        stat.setId(10L);
        stat.setPendingCount(2L);
        stat.setMaxLevel(1);
        when(alarmMapper.countUnverifiedByMerchant()).thenReturn(Collections.singletonList(stat));
        when(merchantMapper.selectList(any())).thenReturn(
                Arrays.asList(merchant(10L, "有报警商户"), merchant(20L, "无报警商户")));

        List<AlarmMerchantVO> result = service.merchantList(null, true);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId().longValue());
        assertEquals(2L, result.get(0).getPendingCount().longValue());
        verify(alarmMapper).countUnverifiedByMerchant();
        verify(alarmMapper, never()).countPendingByMerchant();
    }

    @Test
    void should_return_all_merchants_when_unverified_only_false() {
        when(alarmMapper.countPendingByMerchant()).thenReturn(Collections.emptyList());
        when(merchantMapper.selectList(any())).thenReturn(
                Collections.singletonList(merchant(20L, "无报警商户")));

        List<AlarmMerchantVO> result = service.merchantList(null, false);

        assertEquals(1, result.size());
        assertEquals(0L, result.get(0).getPendingCount().longValue());
        verify(alarmMapper).countPendingByMerchant();
    }

    private Merchant merchant(Long id, String name) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setName(name);
        return merchant;
    }

    private FireAlarm pendingAlarm() {
        FireAlarm alarm = new FireAlarm();
        alarm.setId(5L);
        alarm.setTenantId(1L);
        alarm.setMerchantId(10L);
        alarm.setSn("SN-1");
        alarm.setPosition("1F 大厅");
        alarm.setAlarmLevel(1);
        alarm.setStatus(FireAlarm.STATUS_PENDING);
        alarm.setVerifyStatus(FireAlarm.VERIFY_PENDING);
        return alarm;
    }

    private AlarmMerchantHandleDTO handleRequest(int result) {
        AlarmMerchantHandleDTO request = new AlarmMerchantHandleDTO();
        request.setId(5L);
        request.setResult(result);
        request.setRemark("现场确认");
        return request;
    }

    private AlarmDispatchDTO dispatchRequest() {
        AlarmDispatchDTO request = new AlarmDispatchDTO();
        request.setId(5L);
        request.setWorkAreaId(3L);
        request.setHandlerId(9L);
        request.setDispatchRemark("请到场确认");
        return request;
    }

    private AlarmVerifyDTO verifyRequest() {
        AlarmVerifyDTO request = new AlarmVerifyDTO();
        request.setId(5L);
        request.setResult(FireAlarm.RESULT_TRUE);
        return request;
    }

    private AlarmCallDTO callRequest(boolean connected) {
        AlarmCallDTO request = new AlarmCallDTO();
        request.setId(5L);
        request.setCalledNo("13800138000");
        request.setCallType(1);
        request.setConnected(connected);
        request.setContactName("业主 张三");
        return request;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<LambdaUpdateWrapper<FireAlarm>> wrapperCaptor() {
        return ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }
}
