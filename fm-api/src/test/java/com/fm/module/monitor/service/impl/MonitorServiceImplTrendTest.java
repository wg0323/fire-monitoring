package com.fm.module.monitor.service.impl;

import com.fm.common.exception.BizException;
import com.fm.module.device.entity.Device;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.monitor.mapper.ElectricHistoryMapper;
import com.fm.module.monitor.mapper.ElectricRealtimeMapper;
import com.fm.module.monitor.mapper.WaterHistoryMapper;
import com.fm.module.monitor.mapper.WaterRealtimeMapper;
import com.fm.module.monitor.vo.ElectricTrendRow;
import com.fm.module.monitor.vo.MonitorTrendVO;
import com.fm.module.monitor.vo.WaterTrendRow;
import com.fm.module.system.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 档案趋势：周桶补齐、单位换算与多指标拆分。
 */
class MonitorServiceImplTrendTest {

    private final ElectricRealtimeMapper electricRealtimeMapper = mock(ElectricRealtimeMapper.class);
    private final ElectricHistoryMapper electricHistoryMapper = mock(ElectricHistoryMapper.class);
    private final WaterRealtimeMapper waterRealtimeMapper = mock(WaterRealtimeMapper.class);
    private final WaterHistoryMapper waterHistoryMapper = mock(WaterHistoryMapper.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);

    private MonitorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MonitorServiceImpl(electricRealtimeMapper, electricHistoryMapper, waterRealtimeMapper,
                waterHistoryMapper, deviceMapper, merchantMapper, dataScopeService);
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(dataScopeService.scopedMerchantIds()).thenReturn(null);
    }

    @Test
    void waterTrendScalesPressureAndFillsWeekBuckets() {
        Device device = device(1L, "SN-WATER", "dh-wsm20gb");
        when(deviceMapper.selectById(1L)).thenReturn(device);
        WaterTrendRow row = new WaterTrendRow();
        row.setBucket(LocalDate.now().toString());
        row.setMeasureType(1);
        row.setAvgValue(new BigDecimal("42"));
        row.setMaxValue(new BigDecimal("45"));
        row.setMinValue(new BigDecimal("39"));
        when(waterHistoryMapper.selectTrend(anyString(), anyString(), any(), any()))
                .thenReturn(Collections.singletonList(row));

        List<MonitorTrendVO> result = service.trend(1L, "week");

        assertEquals(1, result.size());
        MonitorTrendVO series = result.get(0);
        assertEquals("pressure", series.getMetric());
        assertEquals("MPa", series.getUnit());
        assertTrue(series.isHasData());
        assertEquals(7, series.getPoints().size());
        assertEquals(new BigDecimal("0.42"), series.getPoints().get(6).getValue());
        assertEquals(new BigDecimal("0.45"), series.getMax());
        assertEquals(new BigDecimal("0.39"), series.getMin());
    }

    @Test
    void electricTrendSplitsMetricsAndScalesTenths() {
        Device device = device(2L, "SN-ELEC", "dh-elec");
        when(deviceMapper.selectById(2L)).thenReturn(device);
        ElectricTrendRow row = new ElectricTrendRow();
        row.setBucket(LocalDate.now().toString());
        row.setVoltageAvg(new BigDecimal("220"));
        row.setVoltageMax(new BigDecimal("225"));
        row.setVoltageMin(new BigDecimal("215"));
        row.setCurrentAvg(new BigDecimal("125"));
        row.setCurrentMax(new BigDecimal("130"));
        row.setCurrentMin(new BigDecimal("120"));
        row.setResidualAvg(new BigDecimal("18"));
        row.setResidualMax(new BigDecimal("20"));
        row.setResidualMin(new BigDecimal("16"));
        row.setTemperatureAvg(new BigDecimal("325"));
        row.setTemperatureMax(new BigDecimal("330"));
        row.setTemperatureMin(new BigDecimal("320"));
        when(electricHistoryMapper.selectTrend(anyString(), anyString(), any(), any()))
                .thenReturn(Collections.singletonList(row));

        List<MonitorTrendVO> result = service.trend(2L, "week");

        assertEquals(4, result.size());
        assertEquals("voltage", result.get(0).getMetric());
        assertEquals(new BigDecimal("225"), result.get(0).getMax());
        assertEquals("current", result.get(1).getMetric());
        assertEquals(new BigDecimal("13.0"), result.get(1).getMax());
        assertEquals("residual", result.get(2).getMetric());
        assertEquals(new BigDecimal("2.0"), result.get(2).getMax());
        assertEquals("temperature", result.get(3).getMetric());
        assertEquals(new BigDecimal("33.0"), result.get(3).getMax());
    }

    @Test
    void archiveTrendRejectsMismatchedDeviceType() {
        when(deviceMapper.selectById(1L)).thenReturn(device(1L, "SN-WATER", "dh-wsm20gb"));

        assertThrows(BizException.class, () -> service.trend(1L, "week", "electric"));
    }

    private Device device(Long id, String sn, String type) {
        Device device = new Device();
        device.setId(id);
        device.setSn(sn);
        device.setType(type);
        device.setMerchantId(9L);
        return device;
    }
}
