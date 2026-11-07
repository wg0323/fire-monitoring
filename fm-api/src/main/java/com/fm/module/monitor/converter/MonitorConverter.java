package com.fm.module.monitor.converter;

import com.fm.module.monitor.entity.ElectricHistory;
import com.fm.module.monitor.entity.ElectricRealtime;
import com.fm.module.monitor.entity.WaterHistory;
import com.fm.module.monitor.entity.WaterRealtime;
import com.fm.module.monitor.vo.MonitorDeviceVO;
import com.fm.module.monitor.vo.MonitorHistoryVO;
import org.springframework.beans.BeanUtils;

/**
 * 实时监测对象转换。
 */
public final class MonitorConverter {

    private MonitorConverter() {
    }

    public static MonitorDeviceVO toDeviceVO(ElectricRealtime row) {
        MonitorDeviceVO vo = new MonitorDeviceVO();
        BeanUtils.copyProperties(row, vo);
        return vo;
    }

    public static MonitorDeviceVO toDeviceVO(WaterRealtime row) {
        MonitorDeviceVO vo = new MonitorDeviceVO();
        BeanUtils.copyProperties(row, vo);
        return vo;
    }

    public static MonitorHistoryVO toHistoryVO(ElectricHistory row) {
        MonitorHistoryVO vo = new MonitorHistoryVO();
        BeanUtils.copyProperties(row, vo);
        return vo;
    }

    public static MonitorHistoryVO toHistoryVO(WaterHistory row) {
        MonitorHistoryVO vo = new MonitorHistoryVO();
        BeanUtils.copyProperties(row, vo);
        return vo;
    }
}
