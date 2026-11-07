package com.fm.module.monitor.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.monitor.dto.MonitorQueryDTO;
import com.fm.module.monitor.vo.MonitorDeviceVO;
import com.fm.module.monitor.vo.MonitorHistoryVO;
import com.fm.module.monitor.vo.MonitorTrendVO;

import java.util.List;

/**
 * 电气/水压实时与历史查询。
 */
public interface MonitorService {

    Page<MonitorDeviceVO> electricPage(MonitorQueryDTO query);

    List<MonitorHistoryVO> electricHistory(Long deviceId, int limit);

    Page<MonitorDeviceVO> waterPage(MonitorQueryDTO query);

    List<MonitorHistoryVO> waterHistory(Long deviceId, int limit);

    /**
     * 档案趋势：按设备类型返回水/电指标的周/月/年序列，无历史数据返回空列表。
     *
     * @param deviceId 设备 id
     * @param range    week / month / year
     */
    List<MonitorTrendVO> trend(Long deviceId, String range);

    /**
     * 档案趋势：校验设备类型与档案类别一致后返回序列。
     *
     * @param deviceId 设备 id
     * @param range    week / month / year
     * @param category water / electric
     */
    List<MonitorTrendVO> trend(Long deviceId, String range, String category);
}
