package com.fm.module.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.module.device.constant.DeviceTypeCodes;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.monitor.entity.ElectricRealtime;
import com.fm.module.monitor.entity.WaterRealtime;
import com.fm.module.monitor.mapper.ElectricRealtimeMapper;
import com.fm.module.monitor.mapper.WaterRealtimeMapper;
import com.fm.module.monitor.converter.MonitorConverter;
import com.fm.module.monitor.service.ArchiveService;
import com.fm.module.monitor.vo.ArchiveBuildingVO;
import com.fm.module.monitor.vo.ArchiveFloorVO;
import com.fm.module.monitor.vo.ArchiveMerchantVO;
import com.fm.module.monitor.vo.MonitorDeviceVO;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 电子档案树：按商户 → 楼宇 → 楼层归集水/电设备，实时值批量补齐，避免逐设备查询。
 */
@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private static final Long EMPTY_MERCHANT_ID = -1L;
    private static final String UNKNOWN_BUILDING = "未知楼宇";
    private static final String UNKNOWN_FLOOR = "未知楼层";

    private final DeviceMapper deviceMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantBuildingMapper buildingMapper;
    private final MerchantFloorMapper floorMapper;
    private final WaterRealtimeMapper waterRealtimeMapper;
    private final ElectricRealtimeMapper electricRealtimeMapper;
    private final DataScopeService dataScopeService;

    @Override
    public List<ArchiveMerchantVO> tree(String category) {
        boolean water = CATEGORY_WATER.equals(category);
        if (!water && !CATEGORY_ELECTRIC.equals(category)) {
            return Collections.emptyList();
        }
        List<Device> devices = devices(water);
        if (devices.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, MonitorDeviceVO> realtime = water ? waterRealtime(devices) : electricRealtime(devices);
        Map<Long, String> merchantNames = merchantNames(devices);
        Map<Long, Map<Long, MerchantBuilding>> buildings = buildings(devices);
        Map<Long, Map<Long, MerchantFloor>> floors = floors(devices);
        Map<Long, ArchiveMerchantVO> result = new LinkedHashMap<>();
        Map<String, ArchiveBuildingVO> buildingNodes = new HashMap<>();
        Map<String, ArchiveFloorVO> floorNodes = new HashMap<>();
        for (Device device : devices) {
            if (device.getMerchantId() == null) {
                continue;
            }
            ArchiveMerchantVO merchant = result.computeIfAbsent(device.getMerchantId(), id -> {
                ArchiveMerchantVO vo = new ArchiveMerchantVO();
                vo.setMerchantId(id);
                vo.setMerchantName(merchantNames.getOrDefault(id, "-"));
                vo.setBuildings(new ArrayList<>());
                vo.setDevices(new ArrayList<>());
                return vo;
            });
            MonitorDeviceVO leaf = leaf(device, realtime.get(device.getSn()));
            if (device.getBuildingId() == null) {
                merchant.getDevices().add(leaf);
                continue;
            }
            String buildingKey = device.getMerchantId() + "-" + device.getBuildingId();
            ArchiveBuildingVO building = buildingNodes.computeIfAbsent(buildingKey,
                    key -> createBuilding(device, buildings));
            if (!merchant.getBuildings().contains(building)) {
                merchant.getBuildings().add(building);
            }
            if (device.getFloorId() == null) {
                building.getDevices().add(leaf);
                continue;
            }
            String floorKey = device.getBuildingId() + "-" + device.getFloorId();
            ArchiveFloorVO floor = floorNodes.computeIfAbsent(floorKey,
                    key -> createFloor(device, floors));
            if (!building.getFloors().contains(floor)) {
                building.getFloors().add(floor);
            }
            floor.getDevices().add(leaf);
        }
        return new ArrayList<>(result.values());
    }

    private ArchiveBuildingVO createBuilding(Device device, Map<Long, Map<Long, MerchantBuilding>> buildings) {
        ArchiveBuildingVO vo = new ArchiveBuildingVO();
        vo.setBuildingId(device.getBuildingId());
        MerchantBuilding building = buildings.getOrDefault(device.getMerchantId(), Collections.emptyMap())
                .get(device.getBuildingId());
        vo.setBuildingName(building == null ? UNKNOWN_BUILDING : building.getName());
        vo.setFloors(new ArrayList<>());
        vo.setDevices(new ArrayList<>());
        return vo;
    }

    private ArchiveFloorVO createFloor(Device device, Map<Long, Map<Long, MerchantFloor>> floors) {
        ArchiveFloorVO vo = new ArchiveFloorVO();
        vo.setFloorId(device.getFloorId());
        MerchantFloor floor = floors.getOrDefault(device.getBuildingId(), Collections.emptyMap())
                .get(device.getFloorId());
        vo.setFloorName(floor == null ? UNKNOWN_FLOOR : floor.getName());
        vo.setDevices(new ArrayList<>());
        return vo;
    }

    private MonitorDeviceVO leaf(Device device, MonitorDeviceVO realtime) {
        MonitorDeviceVO vo = realtime == null ? new MonitorDeviceVO() : realtime;
        vo.setDeviceId(device.getId());
        vo.setName(device.getName());
        vo.setSn(device.getSn());
        vo.setType(device.getType());
        vo.setPosition(device.getPosition());
        vo.setOnlineStatus(device.getOnlineStatus());
        vo.setLastTime(device.getLastTime());
        return vo;
    }

    private List<Device> devices(boolean water) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (water) {
            wrapper.in(Device::getType, DeviceTypeCodes.WATER_TYPES);
        } else {
            wrapper.eq(Device::getType, DeviceTypeCodes.ELECTRIC);
        }
        List<Long> merchantIds = dataScopeService.scopedMerchantIds();
        if (merchantIds != null) {
            if (merchantIds.isEmpty()) {
                wrapper.eq(Device::getMerchantId, EMPTY_MERCHANT_ID);
            } else {
                wrapper.in(Device::getMerchantId, merchantIds);
            }
        }
        wrapper.orderByAsc(Device::getMerchantId).orderByAsc(Device::getBuildingId)
                .orderByAsc(Device::getFloorId).orderByAsc(Device::getId);
        if (!dataScopeService.isPlatform()) {
            return deviceMapper.selectList(wrapper);
        }
        List<Long> tenantIds = dataScopeService.authorizedTenantIds();
        if (tenantIds.isEmpty()) {
            return Collections.emptyList();
        }
        wrapper.in(Device::getTenantId, tenantIds);
        return dataScopeService.withIgnoreTenant(() -> deviceMapper.selectList(wrapper));
    }

    private Map<String, MonitorDeviceVO> waterRealtime(List<Device> devices) {
        List<String> sns = sns(devices);
        if (sns.isEmpty()) {
            return Collections.emptyMap();
        }
        List<WaterRealtime> rows = waterRealtimeMapper.selectList(new LambdaQueryWrapper<WaterRealtime>()
                .in(WaterRealtime::getSn, sns));
        Map<String, MonitorDeviceVO> result = new HashMap<>(rows.size());
        for (WaterRealtime row : rows) {
            result.put(row.getSn(), MonitorConverter.toDeviceVO(row));
        }
        return result;
    }

    private Map<String, MonitorDeviceVO> electricRealtime(List<Device> devices) {
        List<String> sns = sns(devices);
        if (sns.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ElectricRealtime> rows = electricRealtimeMapper.selectList(
                new LambdaQueryWrapper<ElectricRealtime>().in(ElectricRealtime::getSn, sns));
        Map<String, MonitorDeviceVO> result = new HashMap<>(rows.size());
        for (ElectricRealtime row : rows) {
            result.put(row.getSn(), MonitorConverter.toDeviceVO(row));
        }
        return result;
    }

    private List<String> sns(List<Device> devices) {
        return devices.stream().map(Device::getSn).filter(StringUtils::hasText)
                .distinct().collect(Collectors.toList());
    }

    private Map<Long, String> merchantNames(List<Device> devices) {
        List<Long> ids = devices.stream().map(Device::getMerchantId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Merchant> merchants = scopedMerchants(ids);
        return merchants.stream().collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));
    }

    private Map<Long, Map<Long, MerchantBuilding>> buildings(List<Device> devices) {
        List<Long> merchantIds = merchantIds(devices);
        if (merchantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<MerchantBuilding> wrapper = new LambdaQueryWrapper<MerchantBuilding>()
                .in(MerchantBuilding::getMerchantId, merchantIds);
        List<MerchantBuilding> rows = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> buildingMapper.selectList(wrapper))
                : buildingMapper.selectList(wrapper);
        Map<Long, Map<Long, MerchantBuilding>> result = new HashMap<>();
        for (MerchantBuilding row : rows) {
            result.computeIfAbsent(row.getMerchantId(), key -> new HashMap<>()).put(row.getId(), row);
        }
        return result;
    }

    private Map<Long, Map<Long, MerchantFloor>> floors(List<Device> devices) {
        List<Long> merchantIds = merchantIds(devices);
        if (merchantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<MerchantFloor> wrapper = new LambdaQueryWrapper<MerchantFloor>()
                .in(MerchantFloor::getMerchantId, merchantIds);
        List<MerchantFloor> rows = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> floorMapper.selectList(wrapper))
                : floorMapper.selectList(wrapper);
        Map<Long, Map<Long, MerchantFloor>> result = new HashMap<>();
        for (MerchantFloor row : rows) {
            result.computeIfAbsent(row.getBuildingId(), key -> new HashMap<>()).put(row.getId(), row);
        }
        return result;
    }

    private List<Long> merchantIds(List<Device> devices) {
        return devices.stream().map(Device::getMerchantId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
    }

    private List<Merchant> scopedMerchants(List<Long> ids) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>().in(Merchant::getId, ids);
        if (!dataScopeService.isPlatform()) {
            return merchantMapper.selectList(wrapper);
        }
        List<Long> tenantIds = dataScopeService.authorizedTenantIds();
        if (tenantIds.isEmpty()) {
            return Collections.emptyList();
        }
        wrapper.in(Merchant::getTenantId, tenantIds);
        return dataScopeService.withIgnoreTenant(() -> merchantMapper.selectList(wrapper));
    }
}
