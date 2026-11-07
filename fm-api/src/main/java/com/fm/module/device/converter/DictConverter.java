package com.fm.module.device.converter;

import com.fm.module.device.entity.DictAdcode;
import com.fm.module.device.entity.DictAlarmType;
import com.fm.module.device.entity.DictDangerType;
import com.fm.module.device.entity.DictDeviceMaker;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.entity.DictMerchantType;
import com.fm.module.device.vo.DeviceTypeVO;
import com.fm.module.device.vo.DictItemVO;
import com.fm.module.device.vo.DictVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典对象转换。
 */
public final class DictConverter {

    private DictConverter() {
    }

    public static DictVO toVO(DictDeviceMaker item) {
        return fill(item.getId(), item.getCode(), item.getName(), item.getRemark(), item.getStatus());
    }

    public static DictVO toVO(DictAlarmType item) {
        return fill(item.getId(), item.getCode(), item.getName(), item.getRemark(), item.getStatus());
    }

    public static DictVO toVO(DictDangerType item) {
        return fill(item.getId(), item.getCode(), item.getName(), item.getRemark(), item.getStatus());
    }

    public static DictVO toVO(DictMerchantType item) {
        return fill(item.getId(), item.getCode(), item.getName(), item.getRemark(), item.getStatus());
    }

    public static DictVO toVO(DictAdcode item) {
        return fill(item.getId(), item.getCode(), item.getName(), null, 1);
    }

    public static DeviceTypeVO toDeviceTypeVO(DictDeviceType item) {
        DeviceTypeVO vo = new DeviceTypeVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    public static List<DeviceTypeVO> toDeviceTypeVOList(List<DictDeviceType> items) {
        return items.stream().map(DictConverter::toDeviceTypeVO).collect(Collectors.toList());
    }

    public static DictItemVO toItemVO(DictDeviceType item) {
        DictItemVO vo = fillItem(item.getId(), item.getCode(), item.getName(), item.getRemark(), item.getStatus());
        vo.setOfflineThreshold(item.getOfflineThreshold());
        vo.setOfflineUnit(item.getOfflineUnit());
        vo.setMonitorFlag(item.getMonitorFlag());
        vo.setSort(item.getSort());
        return vo;
    }

    public static DictItemVO toItemVO(DictDeviceMaker item) {
        return fillItem(item.getId(), item.getCode(), item.getName(), item.getRemark(), item.getStatus());
    }

    public static DictItemVO toItemVO(DictAlarmType item) {
        return fillItem(item.getId(), item.getCode(), item.getName(), item.getRemark(), item.getStatus());
    }

    public static DictItemVO toItemVO(DictDangerType item) {
        return fillItem(item.getId(), item.getCode(), item.getName(), item.getRemark(), item.getStatus());
    }

    public static DictItemVO toItemVO(DictMerchantType item) {
        return fillItem(item.getId(), item.getCode(), item.getName(), item.getRemark(), item.getStatus());
    }

    private static DictItemVO fillItem(Long id, String code, String name, String remark, Integer status) {
        DictItemVO vo = new DictItemVO();
        vo.setId(id);
        vo.setCode(code);
        vo.setName(name);
        vo.setRemark(remark);
        vo.setStatus(status);
        return vo;
    }

    private static DictVO fill(Long id, String code, String name, String remark, Integer status) {
        DictVO vo = new DictVO();
        vo.setId(id);
        vo.setCode(code);
        vo.setName(name);
        vo.setRemark(remark);
        vo.setStatus(status);
        return vo;
    }
}
