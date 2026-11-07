package com.fm.module.device.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.device.entity.Device;
import com.fm.module.device.vo.DeviceAlarmBriefVO;
import com.fm.module.device.vo.DeviceDetailVO;
import com.fm.module.device.vo.DeviceImportErrorVO;
import com.fm.module.device.vo.DevicePointVO;
import com.fm.module.device.vo.DeviceSnapshotItemVO;
import com.fm.module.device.vo.DeviceVO;
import com.fm.module.device.vo.MerchantDeviceBriefVO;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备对象转换。
 */
public final class DeviceConverter {

    private DeviceConverter() {
    }

    public static DeviceVO toVO(Device device) {
        if (device == null) {
            return null;
        }
        DeviceVO vo = new DeviceVO();
        BeanUtils.copyProperties(device, vo);
        return vo;
    }

    public static List<DeviceVO> toVOList(List<Device> devices) {
        return devices.stream().map(DeviceConverter::toVO).collect(Collectors.toList());
    }

    public static Page<DeviceVO> toVOPage(Page<Device> page) {
        Page<DeviceVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    public static DeviceDetailVO toDetailVO(DeviceVO base) {
        if (base == null) {
            return null;
        }
        DeviceDetailVO vo = new DeviceDetailVO();
        BeanUtils.copyProperties(base, vo);
        return vo;
    }

    public static DeviceAlarmBriefVO toAlarmBriefVO(FireAlarm alarm, String alarmTypeName) {
        if (alarm == null) {
            return null;
        }
        DeviceAlarmBriefVO vo = new DeviceAlarmBriefVO();
        vo.setId(alarm.getId());
        vo.setAlarmTime(alarm.getAlarmTime());
        vo.setAlarmType(alarm.getAlarmType());
        vo.setAlarmTypeName(alarmTypeName);
        vo.setAlarmLevel(alarm.getAlarmLevel());
        vo.setStatus(alarm.getStatus());
        vo.setResult(alarm.getResult());
        vo.setPosition(alarm.getPosition());
        return vo;
    }

    public static DevicePointVO toPointVO(Device device) {
        if (device == null) {
            return null;
        }
        DevicePointVO vo = new DevicePointVO();
        BeanUtils.copyProperties(device, vo);
        return vo;
    }

    public static List<DevicePointVO> toPointVOList(List<Device> devices) {
        return devices.stream().map(DeviceConverter::toPointVO).collect(Collectors.toList());
    }

    public static MerchantDeviceBriefVO toMerchantBriefVO(Device device, String typeName, String locationText) {
        if (device == null) {
            return null;
        }
        MerchantDeviceBriefVO vo = new MerchantDeviceBriefVO();
        vo.setId(device.getId());
        vo.setName(device.getName());
        vo.setSn(device.getSn());
        vo.setType(device.getType());
        vo.setTypeName(typeName);
        vo.setPosition(device.getPosition());
        vo.setOnlineStatus(device.getOnlineStatus());
        vo.setLastTime(device.getLastTime());
        vo.setLocationText(locationText);
        return vo;
    }

    public static DeviceImportErrorVO toImportErrorVO(int rowNum, String sn, String message) {
        DeviceImportErrorVO vo = new DeviceImportErrorVO();
        vo.setRowNum(rowNum);
        vo.setSn(sn);
        vo.setMessage(message);
        return vo;
    }

    public static DeviceSnapshotItemVO toSnapshotItem(String label, String value, LocalDateTime time) {
        DeviceSnapshotItemVO item = new DeviceSnapshotItemVO();
        item.setLabel(label);
        item.setValue(value);
        item.setTime(time);
        return item;
    }
}
