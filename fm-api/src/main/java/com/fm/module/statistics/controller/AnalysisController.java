package com.fm.module.statistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.alarm.vo.AlarmVO;
import com.fm.module.statistics.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统计分析接口。
 */
@Tag(name = "统计分析", description = "报警表格、报警趋势与隐患类型分析")
@RestController
@RequestMapping("/api/admin/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "报警明细表", description = "按时间范围分页返回报警明细")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/alarm/table")
    public Result<Page<AlarmVO>> alarmTable(
            @Parameter(description = "开始日期，格式 yyyy-MM-dd，可为空") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd，可为空") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(analysisService.alarmTable(startDate, endDate, pageNum, pageSize));
    }

    @Operation(summary = "报警趋势", description = "按天返回报警数量趋势")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/alarm/trend")
    public Result<Map<String, Object>> alarmTrend(
            @Parameter(description = "开始日期，格式 yyyy-MM-dd，可为空") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd，可为空") @RequestParam(required = false) String endDate) {
        return Result.ok(analysisService.alarmTrend(startDate, endDate));
    }

    @Operation(summary = "隐患类型分析", description = "按隐患类型聚合数量与占比")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/danger")
    public Result<Map<String, Object>> danger(
            @Parameter(description = "开始日期，格式 yyyy-MM-dd，可为空") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd，可为空") @RequestParam(required = false) String endDate) {
        return Result.ok(analysisService.dangerAnalysis(startDate, endDate));
    }
}
