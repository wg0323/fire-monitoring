package com.fm.module.statistics.controller;

import com.fm.common.api.Result;
import com.fm.module.statistics.dto.StatQueryDTO;
import com.fm.module.statistics.service.StatService;
import com.fm.module.statistics.vo.MonitorHomeVO;
import com.fm.module.statistics.vo.OverviewVO;
import com.fm.module.statistics.vo.StatAlarmVO;
import com.fm.module.statistics.vo.StatCallVO;
import com.fm.module.statistics.vo.StatComprehensiveVO;
import com.fm.module.statistics.vo.StatDangerVO;
import com.fm.module.statistics.vo.StatFaultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营概览统计接口。
 */
@Tag(name = "运营统计", description = "运营概览、报警/故障/隐患/真警/通话的统计分析")
@RestController
@RequestMapping("/api/admin/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @Operation(summary = "运营概览", description = "首页运营 KPI 汇总")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/overview")
    public Result<OverviewVO> overview() {
        return Result.ok(statService.overview());
    }

    @Operation(summary = "值守首页概览", description = "值守台首页所需的监控与待办汇总")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/monitor-home")
    public Result<MonitorHomeVO> monitorHome() {
        return Result.ok(statService.monitorHome());
    }

    @Operation(summary = "综合分析", description = "按时间范围聚合报警、隐患、设备与巡查数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/comprehensive")
    public Result<StatComprehensiveVO> comprehensive(StatQueryDTO query) {
        return Result.ok(statService.comprehensive(query));
    }

    @Operation(summary = "报警分析", description = "按时间范围统计报警数量与趋势")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/alarm")
    public Result<StatAlarmVO> alarm(StatQueryDTO query) {
        return Result.ok(statService.alarm(query));
    }

    @Operation(summary = "故障分析", description = "按时间范围统计设备故障数量与趋势")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/fault")
    public Result<StatFaultVO> fault(StatQueryDTO query) {
        return Result.ok(statService.fault(query));
    }

    @Operation(summary = "隐患分析", description = "按时间范围统计隐患数量、类型与处置情况")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/danger")
    public Result<StatDangerVO> danger(StatQueryDTO query) {
        return Result.ok(statService.danger(query));
    }

    @Operation(summary = "真警分析", description = "按时间范围统计核实为真实火警的数量与占比")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/fire")
    public Result<StatAlarmVO> fire(StatQueryDTO query) {
        return Result.ok(statService.fire(query));
    }

    @Operation(summary = "通话分析", description = "按时间范围统计核警通话量与结果分布")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/call")
    public Result<StatCallVO> call(StatQueryDTO query) {
        return Result.ok(statService.call(query));
    }
}
