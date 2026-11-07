package com.fm.module.statistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.vo.AlarmVO;

import java.util.Map;

/**
 * 统计分析：报警分析与隐患分析。
 */
public interface AnalysisService {

    Page<AlarmVO> alarmTable(String startDate, String endDate, long pageNum, long pageSize);

    Map<String, Object> alarmTrend(String startDate, String endDate);

    Map<String, Object> dangerAnalysis(String startDate, String endDate);
}
