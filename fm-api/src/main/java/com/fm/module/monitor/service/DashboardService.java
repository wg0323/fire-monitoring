package com.fm.module.monitor.service;

import com.fm.module.monitor.vo.ScreenAlarmVO;
import com.fm.module.monitor.vo.ScreenComprehensiveVO;
import com.fm.module.monitor.vo.ScreenDangerVO;
import com.fm.module.monitor.vo.ScreenDeviceVO;

/**
 * 数据大屏聚合查询。
 */
public interface DashboardService {

    ScreenComprehensiveVO comprehensive();

    ScreenAlarmVO alarm(String range);

    ScreenDangerVO danger(String range);

    ScreenDeviceVO device(String range);
}
