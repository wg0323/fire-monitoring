package com.fm.module.monitor.controller;

import com.fm.common.api.Result;
import com.fm.module.monitor.service.DashboardService;
import com.fm.module.monitor.vo.ScreenAlarmVO;
import com.fm.module.monitor.vo.ScreenComprehensiveVO;
import com.fm.module.monitor.vo.ScreenDangerVO;
import com.fm.module.monitor.vo.ScreenDeviceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据大屏接口：4 个页面各自一个聚合读接口。
 */
@Tag(name = "数据大屏", description = "综合监控、报警监控、巡查隐患与设备监控四个大屏聚合读接口")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "综合监控大屏", description = "返回综合监控页所需的全部聚合指标")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/comprehensive")
    public Result<ScreenComprehensiveVO> comprehensive() {
        return Result.ok(dashboardService.comprehensive());
    }

    @Operation(summary = "报警监控大屏", description = "按时间范围返回报警趋势、实时报警与地图点")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/alarm")
    public Result<ScreenAlarmVO> alarm(
            @Parameter(description = "统计范围：day/week/month") @RequestParam(defaultValue = "week") String range) {
        return Result.ok(dashboardService.alarm(range));
    }

    @Operation(summary = "巡查隐患大屏", description = "按时间范围返回巡查与隐患统计")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/danger")
    public Result<ScreenDangerVO> danger(
            @Parameter(description = "统计范围：day/week/month") @RequestParam(defaultValue = "week") String range) {
        return Result.ok(dashboardService.danger(range));
    }

    @Operation(summary = "设备监控大屏", description = "按时间范围返回设备在线、故障与类型统计")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/device")
    public Result<ScreenDeviceVO> device(
            @Parameter(description = "统计范围：day/week/month") @RequestParam(defaultValue = "week") String range) {
        return Result.ok(dashboardService.device(range));
    }
}
