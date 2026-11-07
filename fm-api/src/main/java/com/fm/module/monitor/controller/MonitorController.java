package com.fm.module.monitor.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.monitor.dto.MonitorQueryDTO;
import com.fm.module.monitor.service.MonitorService;
import com.fm.module.monitor.vo.MonitorDeviceVO;
import com.fm.module.monitor.vo.MonitorHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 电气火灾/水压监测接口。
 */
@Tag(name = "实时监测", description = "电气火灾与水压的实时监测设备与历史曲线")
@RestController
@RequestMapping("/api/admin/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @Operation(summary = "电气火灾监测分页", description = "按企业与状态分页返回电气火灾监测设备")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/electric/page")
    public Result<Page<MonitorDeviceVO>> electricPage(MonitorQueryDTO query) {
        return Result.ok(monitorService.electricPage(query));
    }

    @Operation(summary = "电气火灾历史曲线", description = "返回指定设备的最近若干条历史数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/electric/{deviceId}/history")
    public Result<List<MonitorHistoryVO>> electricHistory(
            @Parameter(description = "设备 ID", required = true) @PathVariable Long deviceId,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(monitorService.electricHistory(deviceId, limit));
    }

    @Operation(summary = "水压监测分页", description = "按企业与状态分页返回水压监测设备")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/water/page")
    public Result<Page<MonitorDeviceVO>> waterPage(MonitorQueryDTO query) {
        return Result.ok(monitorService.waterPage(query));
    }

    @Operation(summary = "水压历史曲线", description = "返回指定设备的最近若干条历史数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/water/{deviceId}/history")
    public Result<List<MonitorHistoryVO>> waterHistory(
            @Parameter(description = "设备 ID", required = true) @PathVariable Long deviceId,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(monitorService.waterHistory(deviceId, limit));
    }
}
