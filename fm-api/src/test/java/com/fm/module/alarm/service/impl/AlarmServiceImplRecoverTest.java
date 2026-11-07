package com.fm.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.CallRecordMapper;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.mapper.FireAlarmTraceMapper;
import com.fm.module.alarm.mq.DeviceMessage;
import com.fm.module.alarm.service.AlarmDomainService;
import com.fm.module.alarm.service.AlarmService;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictService;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.DataScopeService;
import com.fm.module.workorder.mapper.WorkOrderMapper;
import com.fm.module.workorder.service.WorkOrderService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 恢复报文收口测试：同一设备可能同时存在火警与故障，恢复必须按 alarmType 找到对应那条，
 * 否则会把没恢复的火警销掉。alarmType 缺失时保留原有的「关闭最近一条」行为。
 */
class AlarmServiceImplRecoverTest {

    private static final String SN = "864852062800536";

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, FireAlarm.class);
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
    void recover_with_alarm_type_filters_by_type() {
        when(alarmMapper.selectOne(any())).thenReturn(null);

        service.handleRecover(message("FAULT"));

        LambdaQueryWrapper<FireAlarm> wrapper = capturedQuery();
        assertTrue(wrapper.getSqlSegment().contains("alarm_type"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("FAULT"));
    }

    @Test
    void recover_without_alarm_type_keeps_previous_behavior() {
        when(alarmMapper.selectOne(any())).thenReturn(null);

        service.handleRecover(message(null));

        LambdaQueryWrapper<FireAlarm> wrapper = capturedQuery();
        assertFalse(wrapper.getSqlSegment().contains("alarm_type"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(SN));
    }

    @Test
    void recover_message_with_recover_alarm_type_is_not_filtered() {
        when(alarmMapper.selectOne(any())).thenReturn(null);

        service.handleRecover(message("RECOVER"));

        LambdaQueryWrapper<FireAlarm> wrapper = capturedQuery();
        assertFalse(wrapper.getSqlSegment().contains("alarm_type"));
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<FireAlarm> capturedQuery() {
        ArgumentCaptor<LambdaQueryWrapper<FireAlarm>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(alarmMapper).selectOne(captor.capture());
        return captor.getValue();
    }

    private DeviceMessage message(String alarmType) {
        DeviceMessage message = new DeviceMessage();
        message.setSn(SN);
        message.setTenantId(1L);
        message.setType("RECOVER");
        message.setEventTime(1791138773496L);
        message.setVersion(1);
        ObjectNode payload = objectMapper.createObjectNode().put("eventCode", 1004);
        if (alarmType != null) {
            payload.put("alarmType", alarmType);
        }
        message.setPayload(payload);
        return message;
    }
}
