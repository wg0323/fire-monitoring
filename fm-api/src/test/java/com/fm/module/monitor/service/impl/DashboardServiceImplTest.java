package com.fm.module.monitor.service.impl;

import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.monitor.vo.MerchantStatusPointVO;
import com.fm.module.monitor.vo.ScreenComprehensiveVO;
import com.fm.module.monitor.vo.ScreenDeviceVO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictService;
import com.fm.module.inspection.mapper.PlanMapper;
import com.fm.module.inspection.mapper.PlanResultMapper;
import com.fm.module.system.service.DataScopeService;
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
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 大屏聚合：设备状态优先级、占比与趋势桶补齐。
 */
class DashboardServiceImplTest {

    private final FireAlarmMapper alarmMapper = mock(FireAlarmMapper.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final DangerMapper dangerMapper = mock(DangerMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final PlanMapper planMapper = mock(PlanMapper.class);
    private final PlanResultMapper planResultMapper = mock(PlanResultMapper.class);
    private final DictService dictService = mock(DictService.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(alarmMapper, deviceMapper, dangerMapper, merchantMapper, planMapper,
                planResultMapper, dictService, dataScopeService);
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(dataScopeService.scopedMerchantIds()).thenReturn(null);
        when(dictService.deviceTypeNames(any())).thenReturn(Collections.emptyMap());
        when(dictService.dangerTypeNames(any())).thenReturn(Collections.emptyMap());
        when(dictService.merchantTypeNames(any())).thenReturn(Collections.emptyMap());
        when(alarmMapper.selectCount(any())).thenReturn(3L);
        when(dangerMapper.selectCount(any())).thenReturn(5L);
        when(planMapper.selectCount(any())).thenReturn(1L);
        when(merchantMapper.selectCount(any())).thenReturn(4L);
        when(planResultMapper.selectMaps(any())).thenReturn(valueRows(2L));
        when(deviceMapper.selectList(any())).thenReturn(devices());
        when(alarmMapper.selectList(any())).thenReturn(alarms());
        when(merchantMapper.selectList(any())).thenReturn(merchants());
    }

    @Test
    void comprehensiveClassifiesDeviceAndMerchantStatus() {
        ScreenComprehensiveVO vo = service.comprehensive();

        assertEquals(Long.valueOf(3L), vo.getFireTotal());
        assertEquals(Long.valueOf(4L), vo.getDeviceTotal());
        assertEquals(Long.valueOf(1L), vo.getDeviceAlarm());
        assertEquals(Long.valueOf(1L), vo.getDeviceFault());
        assertEquals(Long.valueOf(1L), vo.getDeviceOffline());
        assertEquals(Long.valueOf(1L), vo.getDeviceNormal());
        assertEquals(Double.valueOf(25.0), vo.getAlarmPercent());
        assertEquals(Long.valueOf(2L), vo.getPatrolMerchants());
        assertEquals(Double.valueOf(50.0), vo.getPatrolPercent());

        List<MerchantStatusPointVO> points = vo.getMerchantPoints();
        assertEquals(4, points.size());
        assertEquals(Integer.valueOf(1), statusOf(points, 10L));
        assertEquals(Integer.valueOf(2), statusOf(points, 11L));
        assertEquals(Integer.valueOf(3), statusOf(points, 12L));
        assertEquals(Integer.valueOf(0), statusOf(points, 13L));
    }

    @Test
    void comprehensiveSupportsPlatformCrossTenantScope() {
        when(dataScopeService.isPlatform()).thenReturn(true);
        when(dataScopeService.authorizedTenantIds()).thenReturn(Collections.singletonList(1L));
        when(dataScopeService.withIgnoreTenant(any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());

        ScreenComprehensiveVO vo = service.comprehensive();

        assertEquals(Long.valueOf(4L), vo.getDeviceTotal());
        assertEquals(4, vo.getMerchantPoints().size());
    }

    @Test
    void deviceScreenGroupsTypesAndFillsFaultTrend() {
        Map<String, String> typeNames = new HashMap<>(4);
        typeNames.put("dh-wsm20gb", "消火栓水压");
        typeNames.put("dh-elec", "电气火灾");
        when(dictService.deviceTypeNames(any())).thenReturn(typeNames);
        when(alarmMapper.selectList(any())).thenReturn(Collections.singletonList(faultAlarm()));
        Map<String, Object> trendRow = new HashMap<>(2);
        trendRow.put("bucket", LocalDate.now().toString());
        trendRow.put("value", 2L);
        when(alarmMapper.selectMaps(any())).thenReturn(Collections.singletonList(trendRow));

        ScreenDeviceVO vo = service.device("week");

        assertEquals(2, vo.getTypes().size());
        assertEquals(Long.valueOf(2L), vo.getTypes().get(0).getTotal());
        assertEquals(7, vo.getFaultTrend().size());
        assertEquals(Long.valueOf(2L), vo.getFaultTrend().get(6).getValue());
        assertEquals(1, vo.getFaults().size());
        assertEquals("DEV-B", vo.getFaults().get(0).getDeviceSn());
    }

    @Test
    void merchantPointCarriesPopupDetail() {
        List<Merchant> merchants = merchants();
        Merchant merchant = merchants.get(0);
        merchant.setType("001");
        merchant.setPlacePic1("http://192.168.1.20:9000/fm-files/merchant/10/place1.png");
        merchant.setContact1("张三");
        merchant.setPhone1("13800000000");
        when(merchantMapper.selectList(any())).thenReturn(merchants);
        when(dictService.merchantTypeNames(any())).thenReturn(Collections.singletonMap("001", "餐饮"));

        ScreenComprehensiveVO vo = service.comprehensive();

        MerchantStatusPointVO point = vo.getMerchantPoints().stream()
                .filter(item -> Long.valueOf(10L).equals(item.getMerchantId()))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("001", point.getType());
        assertEquals("餐饮", point.getTypeName());
        assertEquals("http://192.168.1.20:9000/fm-files/merchant/10/place1.png", point.getPicUrl());
        assertEquals("张三", point.getContact());
        assertEquals("13800000000", point.getPhone());
        assertEquals(LocalDateTime.of(2026, 11, 5, 10, 0), point.getAlarmTime());
    }

    private Integer statusOf(List<MerchantStatusPointVO> points, Long merchantId) {
        return points.stream().filter(point -> merchantId.equals(point.getMerchantId()))
                .findFirst().map(MerchantStatusPointVO::getStatus).orElse(null);
    }

    private List<Device> devices() {
        List<Device> devices = new ArrayList<>();
        devices.add(device(1L, "DEV-A", "dh-wsm20gb", 10L, 1));
        devices.add(device(2L, "DEV-B", "dh-wsm20gb", 11L, 1));
        devices.add(device(3L, "DEV-C", "dh-elec", 12L, 0));
        devices.add(device(4L, "DEV-D", "dh-elec", 13L, 1));
        return devices;
    }

    private Device device(Long id, String sn, String type, Long merchantId, int onlineStatus) {
        Device device = new Device();
        device.setId(id);
        device.setSn(sn);
        device.setName(sn);
        device.setType(type);
        device.setMerchantId(merchantId);
        device.setStatus(1);
        device.setOnlineStatus(onlineStatus);
        return device;
    }

    private List<FireAlarm> alarms() {
        List<FireAlarm> alarms = new ArrayList<>();
        alarms.add(fireAlarm());
        alarms.add(faultAlarm());
        return alarms;
    }

    private FireAlarm fireAlarm() {
        FireAlarm fire = new FireAlarm();
        fire.setId(1L);
        fire.setSn("DEV-A");
        fire.setMerchantId(10L);
        fire.setAlarmLevel(1);
        fire.setStatus(0);
        fire.setAlarmTime(LocalDateTime.of(2026, 11, 5, 10, 0));
        return fire;
    }

    private FireAlarm faultAlarm() {
        FireAlarm fault = new FireAlarm();
        fault.setId(2L);
        fault.setSn("DEV-B");
        fault.setMerchantId(11L);
        fault.setAlarmLevel(3);
        fault.setStatus(0);
        return fault;
    }

    private List<Merchant> merchants() {
        List<Merchant> merchants = new ArrayList<>();
        merchants.add(merchant(10L, "商户A"));
        merchants.add(merchant(11L, "商户B"));
        merchants.add(merchant(12L, "商户C"));
        merchants.add(merchant(13L, "商户D"));
        return merchants;
    }

    private Merchant merchant(Long id, String name) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setName(name);
        merchant.setStatus(1);
        merchant.setLng(100.0 + id);
        merchant.setLat(30.0 + id);
        return merchant;
    }

    private List<Map<String, Object>> valueRows(Long value) {
        Map<String, Object> row = new HashMap<>(2);
        row.put("value", value);
        return Collections.singletonList(row);
    }
}
