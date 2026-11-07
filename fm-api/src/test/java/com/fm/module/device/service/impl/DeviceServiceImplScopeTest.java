package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.device.dto.DeviceQueryDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.DictDeviceMaker;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.entity.MerchantBuilding;
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
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 设备数据范围：外勤按工作区商户收窄，越权详情不可见。
 */
class DeviceServiceImplScopeTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, Device.class);
        TableInfoHelper.initTableInfo(assistant, DictDeviceType.class);
        TableInfoHelper.initTableInfo(assistant, DictDeviceMaker.class);
        TableInfoHelper.initTableInfo(assistant, Merchant.class);
        TableInfoHelper.initTableInfo(assistant, MerchantBuilding.class);
        TableInfoHelper.initTableInfo(assistant, MerchantFloor.class);
    }

    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final DictDeviceTypeMapper deviceTypeMapper = mock(DictDeviceTypeMapper.class);
    private final DictDeviceMakerMapper deviceMakerMapper = mock(DictDeviceMakerMapper.class);
    private final MerchantFloorMapper floorMapper = mock(MerchantFloorMapper.class);
    private final MerchantBuildingMapper buildingMapper = mock(MerchantBuildingMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final DeviceRegistrySync registrySync = mock(DeviceRegistrySync.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);

    private DeviceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceServiceImpl(deviceTypeMapper, deviceMakerMapper, floorMapper, buildingMapper,
                merchantMapper, registrySync, dataScopeService);
        ReflectionTestUtils.setField(service, "baseMapper", deviceMapper);
        when(dataScopeService.isPlatform()).thenReturn(false);
    }

    @Test
    void pageAddsWorkAreaMerchantCondition() {
        when(dataScopeService.scopedMerchantIds()).thenReturn(Arrays.asList(11L, 12L));
        doReturn(new Page<Device>()).when(deviceMapper).selectPage(any(), any());

        service.page(new DeviceQueryDTO());

        ArgumentCaptor<Wrapper<Device>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(deviceMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("merchant_id"));
    }

    @Test
    void pageReturnsEmptyWhenWorkAreaHasNoMerchant() {
        when(dataScopeService.scopedMerchantIds()).thenReturn(Collections.emptyList());
        doReturn(new Page<Device>()).when(deviceMapper).selectPage(any(), any());

        service.page(new DeviceQueryDTO());

        ArgumentCaptor<Wrapper<Device>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(deviceMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("id ="));
    }

    @Test
    void detailRejectsDeviceOutsideWorkArea() {
        Device device = new Device();
        device.setId(1L);
        device.setMerchantId(99L);
        when(deviceMapper.selectById(1L)).thenReturn(device);
        when(dataScopeService.scopedMerchantIds()).thenReturn(Collections.singletonList(11L));

        assertNull(service.getById(1L));
    }
}
