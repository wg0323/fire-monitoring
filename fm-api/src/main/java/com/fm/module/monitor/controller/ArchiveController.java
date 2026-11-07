package com.fm.module.monitor.controller;

import com.fm.common.api.Result;
import com.fm.module.monitor.service.ArchiveService;
import com.fm.module.monitor.service.MonitorService;
import com.fm.module.monitor.vo.ArchiveMerchantVO;
import com.fm.module.monitor.vo.MonitorTrendVO;
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
 * 电子档案：水系统 / 电气火灾档案树与趋势。
 */
@Tag(name = "电子档案", description = "水系统与电气火灾的档案树与趋势")
@RestController
@RequestMapping("/api/admin/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;
    private final MonitorService monitorService;

    @Operation(summary = "水系统档案树", description = "按企业-建筑-楼层-设备层级返回水系统设备")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/water/tree")
    public Result<List<ArchiveMerchantVO>> waterTree() {
        return Result.ok(archiveService.tree(ArchiveService.CATEGORY_WATER));
    }

    @Operation(summary = "水系统设备趋势", description = "按时间范围返回指定水系统设备的趋势数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/water/{deviceId}/trend")
    public Result<List<MonitorTrendVO>> waterTrend(
            @Parameter(description = "设备 ID", required = true) @PathVariable Long deviceId,
            @Parameter(description = "统计范围：day/week/month") @RequestParam(defaultValue = "week") String range) {
        return Result.ok(monitorService.trend(deviceId, range, ArchiveService.CATEGORY_WATER));
    }

    @Operation(summary = "电气火灾档案树", description = "按企业-建筑-楼层-设备层级返回电气火灾设备")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/electric/tree")
    public Result<List<ArchiveMerchantVO>> electricTree() {
        return Result.ok(archiveService.tree(ArchiveService.CATEGORY_ELECTRIC));
    }

    @Operation(summary = "电气火灾设备趋势", description = "按时间范围返回指定电气火灾设备的趋势数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/electric/{deviceId}/trend")
    public Result<List<MonitorTrendVO>> electricTrend(
            @Parameter(description = "设备 ID", required = true) @PathVariable Long deviceId,
            @Parameter(description = "统计范围：day/week/month") @RequestParam(defaultValue = "week") String range) {
        return Result.ok(monitorService.trend(deviceId, range, ArchiveService.CATEGORY_ELECTRIC));
    }
}
