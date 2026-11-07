package com.fm.module.device.service;

import com.fm.module.device.vo.DeviceDetailVO;
import com.fm.module.monitor.vo.MonitorTrendVO;

import java.util.List;

/**
 * 后台设备详情：快照、趋势与最近报警聚合。
 */
public interface DeviceDetailService {

    DeviceDetailVO detail(Long id, String range);

    List<MonitorTrendVO> trend(Long id, String range);
}
