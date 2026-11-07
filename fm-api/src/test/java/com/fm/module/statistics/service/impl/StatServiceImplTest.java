package com.fm.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.entity.CallRecord;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.CallRecordMapper;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictService;
import com.fm.module.inspection.mapper.PlanResultMapper;
import com.fm.module.statistics.dto.StatQueryDTO;
import com.fm.module.statistics.vo.StatAlarmVO;
import com.fm.module.statistics.vo.StatCallVO;
import com.fm.module.statistics.vo.StatComprehensiveVO;
import com.fm.module.statistics.vo.StatDangerVO;
import com.fm.module.system.service.DataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 统计分析：趋势补桶、故障口径、隐患比率与通话明细关联。
 */
class StatServiceImplTest {

    private final FireAlarmMapper alarmMapper = mock(FireAlarmMapper.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final DangerMapper dangerMapper = mock(DangerMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final PlanResultMapper planResultMapper = mock(PlanResultMapper.class);
    private final CallRecordMapper callRecordMapper = mock(CallRecordMapper.class);
    private final DictService dictService = mock(DictService.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);

    private StatServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, FireAlarm.class);
        TableInfoHelper.initTableInfo(assistant, Danger.class);
        TableInfoHelper.initTableInfo(assistant, CallRecord.class);
        TableInfoHelper.initTableInfo(assistant, Merchant.class);
    }

    @BeforeEach
    void setUp() {
        service = new StatServiceImpl(alarmMapper, deviceMapper, dangerMapper, merchantMapper, planResultMapper,
                callRecordMapper, dictService, dataScopeService);
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(dataScopeService.scopedMerchantIds()).thenReturn(null);
        when(dictService.alarmTypeNames(any())).thenReturn(Collections.emptyMap());
        when(dictService.dangerTypeNames(any())).thenReturn(Collections.emptyMap());
        when(alarmMapper.selectMaps(any())).thenReturn(Collections.emptyList());
        when(alarmMapper.selectCount(any())).thenReturn(2L);
        when(callRecordMapper.selectCount(any())).thenReturn(1L);
        when(callRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(dangerMapper.selectMaps(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void comprehensiveFillsDailyTrendBuckets() {
        StatQueryDTO query = new StatQueryDTO();
        query.setStartTime(LocalDate.now().minusDays(2).atStartOfDay());
        query.setEndTime(LocalDate.now().atTime(23, 59, 59));

        StatComprehensiveVO vo = service.comprehensive(query);

        assertEquals(Long.valueOf(2L), vo.getAlarmTotal());
        assertEquals(Long.valueOf(2L), vo.getFaultTotal());
        assertEquals(Long.valueOf(2L), vo.getFireTotal());
        assertEquals(Long.valueOf(1L), vo.getCallTotal());
        assertEquals(3, vo.getAlarmTrend().size());
        assertEquals(LocalDate.now().minusDays(2).toString(), vo.getAlarmTrend().get(0).getTime());
        assertEquals(Long.valueOf(0L), vo.getAlarmTrend().get(0).getValue());
    }

    @Test
    void dangerAggregatesDoneAndOverdueRates() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(dangerRow("FIRE_FACILITY", 10L, 6L, 2L));
        rows.add(dangerRow("ELECTRIC", 5L, 5L, 0L));
        when(dangerMapper.selectMaps(any())).thenReturn(rows);
        when(dictService.dangerTypeNames(any())).thenReturn(Collections.singletonMap("FIRE_FACILITY", "消防设施"));

        StatDangerVO vo = service.danger(new StatQueryDTO());

        assertEquals(Long.valueOf(15L), vo.getTotal());
        assertEquals(Long.valueOf(11L), vo.getDone());
        assertEquals(Long.valueOf(4L), vo.getUndone());
        assertEquals(Long.valueOf(2L), vo.getOverdue());
        assertEquals(Double.valueOf(73.33), vo.getDoneRate());
        assertEquals(Double.valueOf(13.33), vo.getOverdueRate());
        assertEquals(2, vo.getRows().size());
        assertEquals("消防设施", vo.getRows().get(0).getDangerTypeName());
        assertEquals(Double.valueOf(60.0), vo.getRows().get(0).getDoneRate());
    }

    @Test
    void alarmPageMapsMerchantAndHandleSource() {
        doAnswer(invocation -> {
            Page<FireAlarm> page = invocation.getArgument(0);
            page.setTotal(1L);
            page.setRecords(Collections.singletonList(alarm()));
            return page;
        }).when(alarmMapper).selectPage(any(), any());
        when(merchantMapper.selectList(any())).thenReturn(Collections.singletonList(merchant()));

        StatAlarmVO vo = service.alarm(new StatQueryDTO());

        assertEquals(1, vo.getPage().getRecords().size());
        assertEquals("SN-1", vo.getPage().getRecords().get(0).getSn());
        assertEquals("示范商户", vo.getPage().getRecords().get(0).getMerchantName());
        assertEquals("DUTY", vo.getPage().getRecords().get(0).getHandleSource());
    }

    @Test
    void callPageFallsBackToLocalRecording() {
        CallRecord call = new CallRecord();
        call.setId(1L);
        call.setAlarmId(9L);
        call.setCalledNo("119");
        call.setCallBegin(LocalDateTime.now());
        call.setMonitorFileLocal("/rec/a.mp3");
        doAnswer(invocation -> {
            Page<CallRecord> page = invocation.getArgument(0);
            page.setTotal(1L);
            page.setRecords(Collections.singletonList(call));
            return page;
        }).when(callRecordMapper).selectPage(any(), any());
        when(alarmMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(alarm()));
        when(merchantMapper.selectList(any())).thenReturn(Collections.singletonList(merchant()));
        when(dictService.alarmTypeNames(any())).thenReturn(Collections.singletonMap("FIRE", "火警"));

        StatCallVO vo = service.call(new StatQueryDTO());

        assertEquals(1, vo.getPage().getRecords().size());
        assertEquals("示范商户", vo.getPage().getRecords().get(0).getMerchantName());
        assertEquals("火警", vo.getPage().getRecords().get(0).getAlarmTypeName());
        assertEquals("/rec/a.mp3", vo.getPage().getRecords().get(0).getFileUrl());
        assertEquals(Integer.valueOf(2), vo.getPage().getRecords().get(0).getVerifyStatus());
    }

    @Test
    void callMerchantRankingCountsEveryCall() {
        CallRecord first = new CallRecord();
        first.setAlarmId(9L);
        CallRecord second = new CallRecord();
        second.setAlarmId(9L);
        when(callRecordMapper.selectList(any())).thenReturn(Arrays.asList(first, second));
        when(alarmMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(alarm()));
        when(merchantMapper.selectList(any())).thenReturn(Collections.singletonList(merchant()));

        StatComprehensiveVO vo = service.comprehensive(new StatQueryDTO());

        assertEquals(1, vo.getCallMerchantRanking().size());
        assertEquals("示范商户", vo.getCallMerchantRanking().get(0).getName());
        assertEquals(Long.valueOf(2L), vo.getCallMerchantRanking().get(0).getValue());
    }

    @Test
    void platformWithoutAuthorizedTenantReturnsZero() {
        when(dataScopeService.isPlatform()).thenReturn(true);
        when(dataScopeService.authorizedTenantIds()).thenReturn(Collections.emptyList());

        StatComprehensiveVO vo = service.comprehensive(new StatQueryDTO());

        assertEquals(Long.valueOf(0L), vo.getAlarmTotal());
        assertEquals(7, vo.getAlarmTrend().size());
        assertEquals(Long.valueOf(0L), vo.getAlarmTrend().get(6).getValue());
        assertTrue(vo.getDangerTypeRanking().isEmpty());
        verifyNoInteractions(alarmMapper, dangerMapper, callRecordMapper);
    }

    private Map<String, Object> dangerRow(String type, long total, long done, long overdue) {
        Map<String, Object> row = new HashMap<>(4);
        row.put("type", type);
        row.put("total", total);
        row.put("done", done);
        row.put("overdue", overdue);
        return row;
    }

    private FireAlarm alarm() {
        FireAlarm alarm = new FireAlarm();
        alarm.setId(9L);
        alarm.setSn("SN-1");
        alarm.setMerchantId(10L);
        alarm.setAlarmType("FIRE");
        alarm.setAlarmTime(LocalDateTime.now());
        alarm.setHandleSource("DUTY");
        alarm.setStatus(0);
        alarm.setResult(1);
        alarm.setVerifyStatus(2);
        return alarm;
    }

    private Merchant merchant() {
        Merchant merchant = new Merchant();
        merchant.setId(10L);
        merchant.setName("示范商户");
        return merchant;
    }
}
