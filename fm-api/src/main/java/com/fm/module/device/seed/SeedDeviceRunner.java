package com.fm.module.device.seed;

import com.fm.common.context.TenantContext;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.DictAlarmType;
import com.fm.module.device.entity.DictDangerType;
import com.fm.module.device.entity.DictDeviceMaker;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.entity.DictMerchantType;
import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.DictAlarmTypeMapper;
import com.fm.module.device.mapper.DictDangerTypeMapper;
import com.fm.module.device.mapper.DictDeviceMakerMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.DictMerchantTypeMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * P2 种子数据：字典、示例楼宇楼层与三种定位方式的设备。
 * 已存在设备类型时跳过，可重复执行。
 */
@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class SeedDeviceRunner implements ApplicationRunner {

    private final DictDeviceTypeMapper deviceTypeMapper;
    private final DictDeviceMakerMapper deviceMakerMapper;
    private final DictAlarmTypeMapper alarmTypeMapper;
    private final DictDangerTypeMapper dangerTypeMapper;
    private final DictMerchantTypeMapper merchantTypeMapper;
    private final MerchantBuildingMapper buildingMapper;
    private final MerchantFloorMapper floorMapper;
    private final DeviceMapper deviceMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 幂等判断也必须在忽略租户的上下文中执行，且所有分支都要清理上下文
        TenantContext.set(1L, 1L, null, "TENANT", "TENANT", true);
        try {
            Long count = deviceTypeMapper.selectCount(null);
            if (count != null && count > 0) {
                log.info("device seed data already exists, skip");
                return;
            }
            seedDicts();
            seedBuildings();
            seedDevices();
            log.info("device seed data initialized");
        } finally {
            TenantContext.clear();
        }
    }

    private void seedDicts() {
        insertDeviceType(1L, "dh-sa2ga-a", "大华感烟", 24, "HOUR", 1, 1);
        insertDeviceType(2L, "dh-gb2ga-a", "大华液化气", 24, "HOUR", 1, 2);
        insertDeviceType(3L, "dh-wi2na", "大华水浸", 24, "HOUR", 0, 3);
        insertDeviceType(4L, "dh-wt2ga", "大华感温", 24, "HOUR", 1, 4);
        insertDeviceType(5L, "dh-video", "大华视频", 24, "HOUR", 1, 5);
        insertDeviceType(6L, "dh-cms12gb", "消防终端国标", 12, "HOUR", 1, 6);
        insertDeviceType(7L, "dh-wsm20gb", "消火栓水压", 24, "HOUR", 1, 7);
        insertDeviceType(8L, "gb26875-3", "GB26875.3", 24, "HOUR", 1, 8);
        insertDeviceType(9L, "dh-sa1ga-a", "天翼北向接入", 24, "HOUR", 1, 9);

        insertMaker(1L, "DH", "大华");
        insertMaker(2L, "GB", "国标");

        insertAlarmType(1L, "FIRE", "火警");
        insertAlarmType(2L, "FAULT", "故障");

        insertDangerType(1L, "DANGER_FIRE", "消防隐患");
        insertDangerType(2L, "DANGER_ELECTRIC", "电气隐患");

        insertMerchantType(1L, "NINE_SMALL", "九小场所");
        insertMerchantType(2L, "SOCIAL_UNIT", "社会单位");
    }

    private void seedBuildings() {
        MerchantBuilding building = new MerchantBuilding();
        building.setId(1L);
        building.setTenantId(1L);
        building.setMerchantId(1L);
        building.setName("1号楼");
        building.setSort(1);
        building.setStatus(1);
        buildingMapper.insert(building);

        insertFloor(1L, 1, "1层", 1);
        insertFloor(2L, 2, "2层", 2);
    }

    private void seedDevices() {
        insertDevice(1L, "GPS 感烟", "SEED-GPS-001", "dh-sa2ga-a",
                Device.LOCATION_GPS, null, null, null, null, 120.1, 30.2);
        insertDevice(2L, "楼层感温", "SEED-FLOOR-001", "dh-wt2ga",
                Device.LOCATION_FLOOR, 1L, 1L, new BigDecimal("35.5"), new BigDecimal("62.0"), null, null);
        insertDevice(3L, "未设置液化气", "SEED-NONE-001", "dh-gb2ga-a",
                Device.LOCATION_NONE, null, null, null, null, null, null);
    }

    private void insertDeviceType(Long id, String code, String name, Integer threshold,
                                  String unit, Integer monitor, Integer sort) {
        DictDeviceType item = new DictDeviceType();
        item.setId(id);
        item.setCode(code);
        item.setName(name);
        item.setStatus(1);
        item.setOfflineThreshold(threshold);
        item.setOfflineUnit(unit);
        item.setMonitorFlag(monitor);
        item.setSort(sort);
        deviceTypeMapper.insert(item);
    }

    private void insertMaker(Long id, String code, String name) {
        DictDeviceMaker item = new DictDeviceMaker();
        item.setId(id);
        item.setCode(code);
        item.setName(name);
        item.setStatus(1);
        deviceMakerMapper.insert(item);
    }

    private void insertAlarmType(Long id, String code, String name) {
        DictAlarmType item = new DictAlarmType();
        item.setId(id);
        item.setCode(code);
        item.setName(name);
        item.setStatus(1);
        alarmTypeMapper.insert(item);
    }

    private void insertDangerType(Long id, String code, String name) {
        DictDangerType item = new DictDangerType();
        item.setId(id);
        item.setCode(code);
        item.setName(name);
        item.setStatus(1);
        dangerTypeMapper.insert(item);
    }

    private void insertMerchantType(Long id, String code, String name) {
        DictMerchantType item = new DictMerchantType();
        item.setId(id);
        item.setCode(code);
        item.setName(name);
        item.setStatus(1);
        merchantTypeMapper.insert(item);
    }

    private void insertFloor(Long id, int floorNo, String name, int sort) {
        MerchantFloor floor = new MerchantFloor();
        floor.setId(id);
        floor.setTenantId(1L);
        floor.setBuildingId(1L);
        floor.setMerchantId(1L);
        floor.setName(name);
        floor.setFloorNo(floorNo);
        floor.setSort(sort);
        floor.setStatus(1);
        floorMapper.insert(floor);
    }

    private void insertDevice(Long id, String name, String sn, String type, int locationType,
                              Long buildingId, Long floorId, BigDecimal floorX, BigDecimal floorY,
                              Double lng, Double lat) {
        Device device = new Device();
        device.setId(id);
        device.setTenantId(1L);
        device.setName(name);
        device.setSn(sn);
        device.setType(type);
        device.setMaker("DH");
        device.setLocationType(locationType);
        device.setBuildingId(buildingId);
        device.setFloorId(floorId);
        device.setFloorX(floorX);
        device.setFloorY(floorY);
        device.setLng(lng);
        device.setLat(lat);
        device.setMerchantId(1L);
        device.setChannelNum(1);
        device.setStatus(1);
        device.setOnlineStatus(0);
        deviceMapper.insert(device);
    }
}
