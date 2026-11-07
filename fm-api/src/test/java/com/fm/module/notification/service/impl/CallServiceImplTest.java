package com.fm.module.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.entity.CallRecord;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.CallRecordMapper;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.notification.config.CallProperties;
import com.fm.module.notification.dto.CallRequestDTO;
import com.fm.module.notification.service.CallConfigService;
import com.fm.module.notification.vo.CallResultVO;
import com.fm.module.system.service.DataScopeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 无话机联调：mock 模式不调用第三方，只写通话记录；报警追踪表由通话结果登记统一写入。
 */
class CallServiceImplTest {

    private final FireAlarmMapper alarmMapper = mock(FireAlarmMapper.class);
    private final CallRecordMapper callRecordMapper = mock(CallRecordMapper.class);
    private final CallConfigService callConfigService = mock(CallConfigService.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final CallProperties callProperties = new CallProperties();
    private final CallServiceImpl service = new CallServiceImpl(alarmMapper, callRecordMapper,
            callConfigService, new RestTemplateBuilder(), new ObjectMapper(), callProperties, dataScopeService);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void mockDialOutSkipsThirdParty() {
        TenantContext.clear();
        callProperties.setMock(true);
        FireAlarm alarm = new FireAlarm();
        alarm.setId(1L);
        alarm.setTenantId(1L);
        when(alarmMapper.selectById(1L)).thenReturn(alarm);

        CallRequestDTO request = new CallRequestDTO();
        request.setAlarmId(1L);
        request.setPhone("13800000000");
        request.setType(1);

        CallResultVO result = service.dialOut(request);

        assertTrue(result.getSucceed());
        assertEquals("MOCK 外呼已发起", result.getMessage());
        assertNotNull(result.getActionId());
        verify(callRecordMapper).insert(any(CallRecord.class));
        verifyNoInteractions(callConfigService);
    }

    @Test
    void mockDialOutRejectsFaultAlarm() {
        TenantContext.clear();
        callProperties.setMock(true);
        FireAlarm alarm = new FireAlarm();
        alarm.setId(1L);
        alarm.setTenantId(1L);
        alarm.setAlarmLevel(3);
        when(alarmMapper.selectById(1L)).thenReturn(alarm);

        CallRequestDTO request = new CallRequestDTO();
        request.setAlarmId(1L);
        request.setPhone("13800000000");
        request.setType(1);

        BizException error = assertThrows(BizException.class, () -> service.dialOut(request));

        assertEquals("故障报警只能转隐患，无需电话告警", error.getMessage());
        verifyNoInteractions(callRecordMapper);
    }
}
