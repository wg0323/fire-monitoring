package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fm.module.device.dto.FloorPlanDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.system.service.DataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 楼层平面图替换：旧点位必须清空，设备回到未布点列表。
 */
class MerchantFloorServiceImplTest {

    private static final long FLOOR_ID = 1L;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, MerchantFloor.class);
        TableInfoHelper.initTableInfo(assistant, Device.class);
    }

    private final MerchantFloorMapper floorMapper = mock(MerchantFloorMapper.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final MerchantFloorServiceImpl service = new MerchantFloorServiceImpl(
            mock(MerchantBuildingMapper.class), mock(MerchantMapper.class), deviceMapper, dataScopeService);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", floorMapper);
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(floorMapper.selectById(FLOOR_ID)).thenReturn(floor());
    }

    @Test
    void replacingPlanClearsFloorPoints() {
        service.updatePlan(FLOOR_ID, plan());

        ArgumentCaptor<LambdaUpdateWrapper<Device>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(deviceMapper).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertTrue(sqlSet.contains("location_type"));
        assertTrue(sqlSet.contains("floor_id"));
        assertTrue(sqlSet.contains("floor_x"));
        assertTrue(sqlSet.contains("floor_y"));
    }

    private MerchantFloor floor() {
        MerchantFloor floor = new MerchantFloor();
        floor.setId(FLOOR_ID);
        floor.setMerchantId(10L);
        floor.setBuildingId(20L);
        return floor;
    }

    private FloorPlanDTO plan() {
        FloorPlanDTO request = new FloorPlanDTO();
        request.setPlanUrl("/uploads/floor.png");
        request.setPlanWidth(1600);
        request.setPlanHeight(1000);
        return request;
    }
}
