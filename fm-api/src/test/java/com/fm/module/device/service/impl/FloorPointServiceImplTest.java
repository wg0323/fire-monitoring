package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fm.common.exception.BizException;
import com.fm.module.device.dto.FloorPointsSaveDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.DictDeviceMakerMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DeviceRegistrySync;
import com.fm.module.system.service.DataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 楼层布点测试：楼层归属、设备商户、坐标范围与重复校验。
 */
class FloorPointServiceImplTest {

    private static final long FLOOR_ID = 1L;
    private static final long MERCHANT_ID = 10L;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, Device.class);
        TableInfoHelper.initTableInfo(assistant, MerchantFloor.class);
    }

    private final DictDeviceTypeMapper deviceTypeMapper = mock(DictDeviceTypeMapper.class);
    private final DictDeviceMakerMapper deviceMakerMapper = mock(DictDeviceMakerMapper.class);
    private final MerchantFloorMapper floorMapper = mock(MerchantFloorMapper.class);
    private final MerchantBuildingMapper buildingMapper = mock(MerchantBuildingMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final DeviceRegistrySync registrySync = mock(DeviceRegistrySync.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private DeviceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceServiceImpl(deviceTypeMapper, deviceMakerMapper, floorMapper, buildingMapper,
                merchantMapper, registrySync, dataScopeService);
        ReflectionTestUtils.setField(service, "baseMapper", deviceMapper);
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(floorMapper.selectById(FLOOR_ID)).thenReturn(floor());
    }

    @Test
    void missingFloorIsRejected() {
        when(floorMapper.selectById(99L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.floorPoints(99L));
    }

    @Test
    void duplicateDeviceIsRejected() {
        FloorPointsSaveDTO request = request(point(1L, "10", "20"), point(1L, "30", "40"));

        assertThrows(BizException.class, () -> service.saveFloorPoints(FLOOR_ID, request));

        verify(deviceMapper, never()).selectBatchIds(any());
    }

    @Test
    void outOfRangeCoordinateIsRejected() {
        FloorPointsSaveDTO request = request(point(1L, "101", "20"));

        assertThrows(BizException.class, () -> service.saveFloorPoints(FLOOR_ID, request));
    }

    @Test
    void deviceFromOtherMerchantIsRejected() {
        Device otherMerchantDevice = device(1L, 99L);
        when(deviceMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(otherMerchantDevice));
        FloorPointsSaveDTO request = request(point(1L, "10", "20"));

        assertThrows(BizException.class, () -> service.saveFloorPoints(FLOOR_ID, request));

        verify(deviceMapper, never()).update(any(), any());
    }

    @Test
    void emptyRequestDoesNothing() {
        FloorPointsSaveDTO request = new FloorPointsSaveDTO();
        request.setPoints(Collections.emptyList());
        request.setRemovedDeviceIds(Collections.emptyList());

        service.saveFloorPoints(FLOOR_ID, request);

        verify(deviceMapper, never()).selectBatchIds(any());
    }

    @Test
    void platformFloorPointsIgnoresTenantFilter() {
        when(dataScopeService.isPlatform()).thenReturn(true);
        when(dataScopeService.withIgnoreTenant(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(deviceMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.floorPoints(FLOOR_ID);

        verify(dataScopeService, atLeastOnce()).withIgnoreTenant(any());
    }

    private MerchantFloor floor() {
        MerchantFloor floor = new MerchantFloor();
        floor.setId(FLOOR_ID);
        floor.setMerchantId(MERCHANT_ID);
        floor.setBuildingId(100L);
        floor.setName("1F");
        return floor;
    }

    private Device device(Long id, Long merchantId) {
        Device device = new Device();
        device.setId(id);
        device.setMerchantId(merchantId);
        return device;
    }

    private FloorPointsSaveDTO request(FloorPointsSaveDTO.Point... points) {
        FloorPointsSaveDTO request = new FloorPointsSaveDTO();
        request.setPoints(Arrays.asList(points));
        request.setRemovedDeviceIds(Collections.emptyList());
        return request;
    }

    private FloorPointsSaveDTO.Point point(Long deviceId, String x, String y) {
        FloorPointsSaveDTO.Point point = new FloorPointsSaveDTO.Point();
        point.setDeviceId(deviceId);
        point.setX(new BigDecimal(x));
        point.setY(new BigDecimal(y));
        return point;
    }
}
