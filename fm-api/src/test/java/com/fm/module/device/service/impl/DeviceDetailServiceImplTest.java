package com.fm.module.device.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.device.entity.DeviceRealtime;
import com.fm.module.device.entity.DictAlarmType;
import com.fm.module.device.mapper.DeviceRealtimeMapper;
import com.fm.module.device.mapper.DictAlarmTypeMapper;
import com.fm.module.device.service.DeviceService;
import com.fm.module.device.vo.DeviceDetailVO;
import com.fm.module.device.vo.DeviceVO;
import com.fm.module.monitor.entity.WaterRealtime;
import com.fm.module.monitor.mapper.ElectricRealtimeMapper;
import com.fm.module.monitor.mapper.WaterRealtimeMapper;
import com.fm.module.monitor.service.MonitorService;
import com.fm.module.system.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 设备详情：水表快照换算、JSON 快照展开与最近报警类型名。
 */
class DeviceDetailServiceImplTest {

    private final DeviceService deviceService = mock(DeviceService.class);
    private final MonitorService monitorService = mock(MonitorService.class);
    private final WaterRealtimeMapper waterRealtimeMapper = mock(WaterRealtimeMapper.class);
    private final ElectricRealtimeMapper electricRealtimeMapper = mock(ElectricRealtimeMapper.class);
    private final DeviceRealtimeMapper deviceRealtimeMapper = mock(DeviceRealtimeMapper.class);
    private final FireAlarmMapper fireAlarmMapper = mock(FireAlarmMapper.class);
    private final DictAlarmTypeMapper dictAlarmTypeMapper = mock(DictAlarmTypeMapper.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);

    private DeviceDetailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceDetailServiceImpl(deviceService, monitorService, waterRealtimeMapper,
                electricRealtimeMapper, deviceRealtimeMapper, fireAlarmMapper, dictAlarmTypeMapper,
                dataScopeService, new ObjectMapper());
        when(monitorService.trend(any(), any())).thenReturn(Collections.emptyList());
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(fireAlarmMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void waterSnapshotScalesPressure() {
        when(deviceService.getVOById(1L)).thenReturn(device("SN-WATER", "dh-wsm20gb"));
        WaterRealtime realtime = new WaterRealtime();
        realtime.setSn("SN-WATER");
        realtime.setMeasureValue(42);
        realtime.setMeasureType(1);
        realtime.setMeasureTime(LocalDateTime.now());
        when(waterRealtimeMapper.selectOne(any())).thenReturn(realtime);

        DeviceDetailVO detail = service.detail(1L, "week");

        assertEquals(1, detail.getSnapshot().size());
        assertEquals("水压", detail.getSnapshot().get(0).getLabel());
        assertEquals("0.42", detail.getSnapshot().get(0).getValue());
        assertEquals("MPa", detail.getSnapshot().get(0).getUnit());
    }

    @Test
    void jsonSnapshotAndAlarmTypeNameAreResolved() {
        when(deviceService.getVOById(1L)).thenReturn(device("SN-SMOKE", "dh-sa2ga-a"));
        DeviceRealtime realtime = new DeviceRealtime();
        realtime.setSn("SN-SMOKE");
        realtime.setData("{\"status\":\"normal\",\"battery\":95}");
        realtime.setReportTime(LocalDateTime.now());
        when(deviceRealtimeMapper.selectOne(any())).thenReturn(realtime);

        FireAlarm alarm = new FireAlarm();
        alarm.setId(88L);
        alarm.setAlarmType("FIRE");
        alarm.setAlarmTime(LocalDateTime.now());
        when(fireAlarmMapper.selectList(any())).thenReturn(Collections.singletonList(alarm));
        DictAlarmType type = new DictAlarmType();
        type.setCode("FIRE");
        type.setName("火警");
        when(dictAlarmTypeMapper.selectList(any())).thenReturn(Collections.singletonList(type));

        DeviceDetailVO detail = service.detail(1L, "week");

        assertEquals(2, detail.getSnapshot().size());
        assertEquals("status", detail.getSnapshot().get(0).getLabel());
        assertEquals("normal", detail.getSnapshot().get(0).getValue());
        assertEquals(1, detail.getRecentAlarms().size());
        assertEquals("火警", detail.getRecentAlarms().get(0).getAlarmTypeName());
    }

    private DeviceVO device(String sn, String type) {
        DeviceVO vo = new DeviceVO();
        vo.setId(1L);
        vo.setSn(sn);
        vo.setType(type);
        vo.setMerchantId(9L);
        return vo;
    }
}
