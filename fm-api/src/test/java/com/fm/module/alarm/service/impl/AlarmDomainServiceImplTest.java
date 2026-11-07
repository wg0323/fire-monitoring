package com.fm.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.entity.FireAlarmTrace;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.mapper.FireAlarmTraceMapper;
import com.fm.module.alarm.service.AlarmDomainService;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.danger.mapper.DangerTraceMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报警领域规则测试：收口幂等、转隐患幂等、1 小时窗口与自动转判定。
 */
class AlarmDomainServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, FireAlarm.class);
        TableInfoHelper.initTableInfo(assistant, Danger.class);
    }

    private final FireAlarmMapper alarmMapper = mock(FireAlarmMapper.class);
    private final FireAlarmTraceMapper traceMapper = mock(FireAlarmTraceMapper.class);
    private final DangerMapper dangerMapper = mock(DangerMapper.class);
    private final DangerTraceMapper dangerTraceMapper = mock(DangerTraceMapper.class);
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper = mock(SysWorkAreaMerchantMapper.class);
    private final AlarmDomainServiceImpl service = new AlarmDomainServiceImpl(alarmMapper, traceMapper,
            dangerMapper, dangerTraceMapper, workAreaMerchantMapper, new ObjectMapper());

    @AfterEach
    void tearDown() {
        com.fm.common.context.TenantContext.clear();
    }

    @Test
    void finalizeRejectsHandledAlarm() {
        when(alarmMapper.selectById(1L)).thenReturn(alarm(FireAlarm.STATUS_HANDLED, LocalDateTime.now()));

        BizException error = assertThrows(BizException.class, () -> service.finalizeAlarm(
                1L, FireAlarm.RESULT_TRUE, FireAlarm.SOURCE_DUTY, 9L, "值守", null, null, null, null));

        assertEquals("该报警已处理", error.getMessage());
        verify(alarmMapper, never()).update(isNull(), any());
    }

    @Test
    void should_write_verify_trace_when_alarm_finalized() {
        FireAlarm alarm = alarm(FireAlarm.STATUS_PENDING, LocalDateTime.now());
        when(alarmMapper.selectById(1L)).thenReturn(alarm);
        when(alarmMapper.update(isNull(), any())).thenReturn(1);

        service.finalizeAlarm(1L, FireAlarm.RESULT_TRUE, FireAlarm.SOURCE_DUTY,
                9L, "值守", "电话确认火警", null, null, null);

        ArgumentCaptor<FireAlarmTrace> captor = ArgumentCaptor.forClass(FireAlarmTrace.class);
        verify(traceMapper).insert(captor.capture());
        assertEquals(FireAlarmTrace.TYPE_VERIFY, captor.getValue().getTraceType());
        assertEquals(1, captor.getValue().getResult());
    }

    @Test
    void finalizeFaultWritesDeadlineFromAlarmTime() {
        LocalDateTime alarmTime = LocalDateTime.now().minusMinutes(20);
        FireAlarm alarm = alarm(FireAlarm.STATUS_PENDING, alarmTime);
        when(alarmMapper.selectById(1L)).thenReturn(alarm);
        when(alarmMapper.update(isNull(), any())).thenReturn(1);

        service.finalizeAlarm(1L, FireAlarm.RESULT_FAULT, FireAlarm.SOURCE_FIELD,
                9L, "外勤", "现场确认", "p1", null, null);

        ArgumentCaptor<LambdaUpdateWrapper<FireAlarm>> captor = wrapperCaptor();
        verify(alarmMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("danger_convert_deadline"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(alarmTime.plusHours(1)));
        verify(traceMapper).insert(any());
    }

    @Test
    void finalizeFalseClearsDeadline() {
        LocalDateTime alarmTime = LocalDateTime.now().minusMinutes(20);
        FireAlarm alarm = alarm(FireAlarm.STATUS_PENDING, alarmTime);
        alarm.setDangerConvertDeadline(alarmTime.plusHours(1));
        when(alarmMapper.selectById(1L)).thenReturn(alarm);
        when(alarmMapper.update(isNull(), any())).thenReturn(1);

        service.finalizeAlarm(1L, FireAlarm.RESULT_FALSE, FireAlarm.SOURCE_DUTY,
                9L, "值守", null, null, null, null);

        ArgumentCaptor<LambdaUpdateWrapper<FireAlarm>> captor = wrapperCaptor();
        verify(alarmMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("danger_convert_deadline"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(null));
    }

    @Test
    void convertDangerReturnsExistingDangerOnDuplicateKey() {
        LocalDateTime alarmTime = LocalDateTime.now().minusHours(2);
        FireAlarm alarm = alarm(FireAlarm.STATUS_PENDING, alarmTime);
        alarm.setAlarmLevel(3);
        alarm.setDangerConvertDeadline(alarmTime.plusHours(1));
        when(alarmMapper.selectById(1L)).thenReturn(alarm);
        when(dangerMapper.insert(any(Danger.class))).thenThrow(new DuplicateKeyException("uk_danger_source_alarm"));
        Danger existing = new Danger();
        existing.setId(99L);
        when(dangerMapper.selectOne(any())).thenReturn(existing);
        when(alarmMapper.update(isNull(), any())).thenReturn(1);

        Long dangerId = service.convertDanger(1L, "MANUAL", 9L, "值守");

        assertEquals(99L, dangerId);
        verify(dangerMapper, times(1)).insert(any(Danger.class));
        verify(dangerTraceMapper, times(1)).insert(any());
    }

    @Test
    void convertDangerRejectsSelfRecoveredAlarm() {
        LocalDateTime alarmTime = LocalDateTime.now().minusMinutes(40);
        FireAlarm alarm = alarm(FireAlarm.STATUS_PENDING, alarmTime);
        alarm.setAlarmLevel(3);
        alarm.setDangerConvertDeadline(alarmTime.plusHours(1));
        alarm.setRecoverTime(alarmTime.plusMinutes(30));
        when(alarmMapper.selectById(1L)).thenReturn(alarm);

        BizException error = assertThrows(BizException.class,
                () -> service.convertDanger(1L, "MANUAL", 9L, "值守"));

        assertEquals("该故障已自行恢复，不转隐患", error.getMessage());
        verify(dangerMapper, never()).insert(any(Danger.class));
    }

    @Test
    void recoverInWindowClosesAlarmWithoutDanger() {
        LocalDateTime alarmTime = LocalDateTime.now().minusMinutes(30);
        FireAlarm alarm = alarm(FireAlarm.STATUS_PENDING, alarmTime);
        alarm.setAlarmLevel(3);
        alarm.setDangerConvertDeadline(alarmTime.plusHours(1));
        alarm.setRecoverTime(alarmTime.plusMinutes(40));
        when(alarmMapper.selectById(1L)).thenReturn(alarm);
        when(alarmMapper.update(isNull(), any())).thenReturn(1);

        assertTrue(service.recoverInWindow(1L));

        ArgumentCaptor<LambdaUpdateWrapper<FireAlarm>> captor = wrapperCaptor();
        verify(alarmMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("result"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(FireAlarm.SOURCE_SYSTEM));
        verify(traceMapper).insert(any());
    }

    @Test
    void recoverAfterDeadlineDoesNotCloseAlarm() {
        LocalDateTime alarmTime = LocalDateTime.now().minusHours(2);
        FireAlarm alarm = alarm(FireAlarm.STATUS_PENDING, alarmTime);
        alarm.setAlarmLevel(3);
        alarm.setDangerConvertDeadline(alarmTime.plusHours(1));
        alarm.setRecoverTime(alarmTime.plusHours(3));
        when(alarmMapper.selectById(1L)).thenReturn(alarm);

        assertFalse(service.recoverInWindow(1L));

        verify(alarmMapper, never()).update(isNull(), any());
    }

    @Test
    void autoConvertOnlyForDueDeadlineWithNoDanger() {
        LocalDateTime now = LocalDateTime.now();
        FireAlarm due = alarm(FireAlarm.STATUS_PENDING, now.minusHours(2));
        due.setAlarmLevel(3);
        due.setDangerConvertDeadline(now.minusMinutes(10));

        FireAlarm history = alarm(FireAlarm.STATUS_PENDING, now.minusDays(10));
        history.setAlarmLevel(3);
        history.setDangerConvertDeadline(null);

        FireAlarm recoveredInWindow = alarm(FireAlarm.STATUS_PENDING, now.minusHours(2));
        recoveredInWindow.setAlarmLevel(3);
        recoveredInWindow.setDangerConvertDeadline(now.minusMinutes(30));
        recoveredInWindow.setRecoverTime(now.minusMinutes(60));

        FireAlarm converted = alarm(FireAlarm.STATUS_HANDLED, now.minusHours(2));
        converted.setAlarmLevel(3);
        converted.setDangerConvertDeadline(now.minusMinutes(10));
        converted.setDangerId(8L);

        assertTrue(AlarmDomainService.shouldAutoConvert(due, now));
        assertFalse(AlarmDomainService.shouldAutoConvert(history, now));
        assertFalse(AlarmDomainService.shouldAutoConvert(recoveredInWindow, now));
        assertFalse(AlarmDomainService.shouldAutoConvert(converted, now));
    }

    @Test
    void faultDetectionCoversDeviceAndManualFault() {
        FireAlarm deviceFault = alarm(FireAlarm.STATUS_PENDING, LocalDateTime.now());
        deviceFault.setAlarmLevel(3);
        FireAlarm manualFault = alarm(FireAlarm.STATUS_PENDING, LocalDateTime.now());
        manualFault.setAlarmLevel(1);
        manualFault.setResult(FireAlarm.RESULT_FAULT);
        FireAlarm falseAlarm = alarm(FireAlarm.STATUS_HANDLED, LocalDateTime.now());
        falseAlarm.setAlarmLevel(3);
        falseAlarm.setResult(FireAlarm.RESULT_FALSE);
        FireAlarm fire = alarm(FireAlarm.STATUS_PENDING, LocalDateTime.now());

        assertTrue(AlarmDomainService.isFault(deviceFault));
        assertTrue(AlarmDomainService.isFault(manualFault));
        assertFalse(AlarmDomainService.isFault(falseAlarm));
        assertFalse(AlarmDomainService.isFault(fire));
        assertNotNull(deviceFault.getAlarmTime());
    }

    private FireAlarm alarm(int status, LocalDateTime alarmTime) {
        FireAlarm alarm = new FireAlarm();
        alarm.setId(1L);
        alarm.setTenantId(1L);
        alarm.setMerchantId(10L);
        alarm.setStatus(status);
        alarm.setAlarmLevel(1);
        alarm.setAlarmTime(alarmTime);
        return alarm;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<LambdaUpdateWrapper<FireAlarm>> wrapperCaptor() {
        return ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }
}
