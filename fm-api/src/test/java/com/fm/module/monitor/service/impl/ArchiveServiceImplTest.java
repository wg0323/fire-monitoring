package com.fm.module.monitor.service.impl;

import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.monitor.entity.WaterRealtime;
import com.fm.module.monitor.mapper.ElectricRealtimeMapper;
import com.fm.module.monitor.mapper.WaterRealtimeMapper;
import com.fm.module.monitor.vo.ArchiveMerchantVO;
import com.fm.module.system.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 电子档案树：商户 → 楼宇 → 楼层归集与实时值补齐。
 */
class ArchiveServiceImplTest {

    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final MerchantBuildingMapper buildingMapper = mock(MerchantBuildingMapper.class);
    private final MerchantFloorMapper floorMapper = mock(MerchantFloorMapper.class);
    private final WaterRealtimeMapper waterRealtimeMapper = mock(WaterRealtimeMapper.class);
    private final ElectricRealtimeMapper electricRealtimeMapper = mock(ElectricRealtimeMapper.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);

    private ArchiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArchiveServiceImpl(deviceMapper, merchantMapper, buildingMapper, floorMapper,
                waterRealtimeMapper, electricRealtimeMapper, dataScopeService);
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(dataScopeService.scopedMerchantIds()).thenReturn(null);
    }

    @Test
    void treeGroupsMerchantBuildingFloorAndFillsRealtime() {
        Device device = new Device();
        device.setId(1L);
        device.setMerchantId(10L);
        device.setBuildingId(100L);
        device.setFloorId(1000L);
        device.setName("1F 消火栓");
        device.setSn("WATER-1");
        device.setType("dh-wsm20gb");
        device.setOnlineStatus(1);
        when(deviceMapper.selectList(any())).thenReturn(Collections.singletonList(device));

        Merchant merchant = new Merchant();
        merchant.setId(10L);
        merchant.setName("测试商户");
        when(merchantMapper.selectList(any())).thenReturn(Collections.singletonList(merchant));

        MerchantBuilding building = new MerchantBuilding();
        building.setId(100L);
        building.setMerchantId(10L);
        building.setName("1号楼");
        when(buildingMapper.selectList(any())).thenReturn(Collections.singletonList(building));

        MerchantFloor floor = new MerchantFloor();
        floor.setId(1000L);
        floor.setBuildingId(100L);
        floor.setMerchantId(10L);
        floor.setName("1F");
        when(floorMapper.selectList(any())).thenReturn(Collections.singletonList(floor));

        WaterRealtime realtime = new WaterRealtime();
        realtime.setSn("WATER-1");
        realtime.setMeasureValue(42);
        realtime.setMeasureType(1);
        when(waterRealtimeMapper.selectList(any())).thenReturn(Collections.singletonList(realtime));

        List<ArchiveMerchantVO> tree = service.tree("water");

        assertEquals(1, tree.size());
        ArchiveMerchantVO merchantNode = tree.get(0);
        assertEquals("测试商户", merchantNode.getMerchantName());
        assertTrue(merchantNode.getDevices().isEmpty());
        assertEquals(1, merchantNode.getBuildings().size());
        assertEquals(1, merchantNode.getBuildings().get(0).getFloors().size());
        assertEquals(1, merchantNode.getBuildings().get(0).getFloors().get(0).getDevices().size());
        assertEquals(Integer.valueOf(42),
                merchantNode.getBuildings().get(0).getFloors().get(0).getDevices().get(0).getMeasureValue());
        assertEquals("dh-wsm20gb",
                merchantNode.getBuildings().get(0).getFloors().get(0).getDevices().get(0).getType());
    }

    @Test
    void treeWithoutDevicesReturnsEmpty() {
        when(deviceMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertTrue(service.tree("electric").isEmpty());
    }

    @Test
    void treeSupportsPlatformCrossTenantScope() {
        when(dataScopeService.isPlatform()).thenReturn(true);
        when(dataScopeService.authorizedTenantIds()).thenReturn(Collections.singletonList(1L));
        when(dataScopeService.withIgnoreTenant(any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        Device device = new Device();
        device.setId(1L);
        device.setMerchantId(10L);
        device.setName("1F 消火栓");
        device.setSn("WATER-1");
        device.setType("dh-wsm20gb");
        when(deviceMapper.selectList(any())).thenReturn(Collections.singletonList(device));
        Merchant merchant = new Merchant();
        merchant.setId(10L);
        merchant.setName("测试商户");
        when(merchantMapper.selectList(any())).thenReturn(Collections.singletonList(merchant));
        when(waterRealtimeMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<ArchiveMerchantVO> tree = service.tree("water");

        assertEquals(1, tree.size());
        assertEquals(1, tree.get(0).getDevices().size());
        assertEquals("WATER-1", tree.get(0).getDevices().get(0).getSn());
    }
}
