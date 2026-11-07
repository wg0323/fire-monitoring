package com.fm.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.dto.AlarmQueryDTO;
import com.fm.module.alarm.service.AlarmService;
import com.fm.module.alarm.vo.AlarmVO;
import com.fm.module.statistics.service.AnalysisService;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计分析实现：报警表格/趋势与隐患按类型聚合，数据量小，直接内存聚合。
 */
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final AlarmService alarmService;
    private final DangerMapper dangerMapper;
    private final DataScopeService dataScopeService;

    @Override
    public Page<AlarmVO> alarmTable(String startDate, String endDate, long pageNum, long pageSize) {
        AlarmQueryDTO query = new AlarmQueryDTO();
        query.setPageNum((int) pageNum);
        query.setPageSize((int) pageSize);
        query.setStartTime(parseDate(startDate, LocalDate.now().withDayOfMonth(1)).atStartOfDay());
        query.setEndTime(parseDate(endDate, LocalDate.now()).plusDays(1).atStartOfDay().minusSeconds(1));
        return alarmService.pageVO(query);
    }

    @Override
    public Map<String, Object> alarmTrend(String startDate, String endDate) {
        LocalDate start = parseDate(startDate, LocalDate.now().withDayOfMonth(1));
        LocalDate end = parseDate(endDate, LocalDate.now());
        AlarmQueryDTO query = new AlarmQueryDTO();
        query.setPageNum(1);
        query.setPageSize(1000);
        query.setStartTime(start.atStartOfDay());
        query.setEndTime(end.plusDays(1).atStartOfDay().minusSeconds(1));
        List<AlarmVO> rows = alarmService.pageVO(query).getRecords();

        Map<String, Long> counts = new HashMap<>();
        for (AlarmVO row : rows) {
            if (row.getAlarmTime() != null) {
                counts.merge(row.getAlarmTime().toLocalDate().toString(), 1L, Long::sum);
            }
        }
        List<String> xAxis = new ArrayList<>();
        List<Long> yAxis = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            xAxis.add(day.toString());
            yAxis.add(counts.getOrDefault(day.toString(), 0L));
        }
        Map<String, Object> trend = new HashMap<>();
        trend.put("xAxis", xAxis);
        trend.put("yAxis", yAxis);
        Map<String, Object> result = new HashMap<>();
        result.put("trend", trend);
        return result;
    }

    @Override
    public Map<String, Object> dangerAnalysis(String startDate, String endDate) {
        LocalDate start = parseDate(startDate, LocalDate.now().withDayOfMonth(1));
        LocalDate end = parseDate(endDate, LocalDate.now());
        LambdaQueryWrapper<Danger> wrapper = new LambdaQueryWrapper<Danger>()
                .ge(Danger::getCreateTime, start.atStartOfDay())
                .lt(Danger::getCreateTime, end.plusDays(1).atStartOfDay())
                .orderByDesc(Danger::getCreateTime);

        List<Danger> rows;
        if (dataScopeService.isPlatform()) {
            rows = dataScopeService.withIgnoreTenant(() -> dangerMapper.selectList(wrapper));
        } else {
            rows = dangerMapper.selectList(wrapper);
        }

        LocalDate today = LocalDate.now();
        Map<String, int[]> grouped = new LinkedHashMap<>();
        int total = 0;
        int complete = 0;
        int uncomplete = 0;
        int expect = 0;
        for (Danger danger : rows) {
            String type = StringUtils.hasText(danger.getDangerType()) ? danger.getDangerType() : "其他";
            int[] counters = grouped.computeIfAbsent(type, key -> new int[4]);
            counters[0]++;
            total++;
            boolean done = Integer.valueOf(Danger.STATUS_DONE).equals(danger.getStatus());
            if (done) {
                counters[1]++;
                complete++;
            } else {
                counters[2]++;
                uncomplete++;
            }
            if (!done && danger.getDealLimit() != null && danger.getDealLimit().isBefore(today)) {
                counters[3]++;
                expect++;
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : grouped.entrySet()) {
            int[] counters = entry.getValue();
            Map<String, Object> item = new HashMap<>();
            item.put("typeName", entry.getKey());
            item.put("totalNum", counters[0]);
            item.put("completeNum", counters[1]);
            item.put("uncompleteNum", counters[2]);
            item.put("expectNum", counters[3]);
            item.put("completeRate", rate(counters[1], counters[0]));
            item.put("expectRate", rate(counters[3], counters[0]));
            list.add(item);
        }

        Map<String, Object> totalData = new HashMap<>();
        totalData.put("totalNum", total);
        totalData.put("completeNum", complete);
        totalData.put("uncompleteNum", uncomplete);
        totalData.put("expectNum", expect);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", totalData);
        return result;
    }

    private double rate(int num, int total) {
        return total <= 0 ? 0.0 : Math.round(num * 10000.0 / total) / 100.0;
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (Exception e) {
            return fallback;
        }
    }
}
