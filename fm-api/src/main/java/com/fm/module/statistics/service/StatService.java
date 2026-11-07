package com.fm.module.statistics.service;

import com.fm.module.statistics.dto.StatQueryDTO;
import com.fm.module.statistics.vo.MonitorHomeVO;
import com.fm.module.statistics.vo.OverviewVO;
import com.fm.module.statistics.vo.StatAlarmVO;
import com.fm.module.statistics.vo.StatCallVO;
import com.fm.module.statistics.vo.StatComprehensiveVO;
import com.fm.module.statistics.vo.StatDangerVO;
import com.fm.module.statistics.vo.StatFaultVO;

/**
 * 运营概览统计与统计分析。
 */
public interface StatService {

    OverviewVO overview();

    MonitorHomeVO monitorHome();

    StatComprehensiveVO comprehensive(StatQueryDTO query);

    StatAlarmVO alarm(StatQueryDTO query);

    StatFaultVO fault(StatQueryDTO query);

    StatDangerVO danger(StatQueryDTO query);

    StatAlarmVO fire(StatQueryDTO query);

    StatCallVO call(StatQueryDTO query);
}
