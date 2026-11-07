package com.fm.module.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.device.dto.DeviceQueryDTO;
import com.fm.module.device.dto.DeviceSaveDTO;
import com.fm.module.device.service.DeviceDetailService;
import com.fm.module.device.service.DeviceExcelService;
import com.fm.module.device.service.DeviceService;
import com.fm.module.device.vo.DeviceDetailVO;
import com.fm.module.device.vo.DeviceImportResultVO;
import com.fm.module.device.vo.DeviceVO;
import com.fm.module.monitor.vo.MonitorTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 设备管理接口。
 */
@Tag(name = "设备管理", description = "设备的查询、新增、修改、删除与导入导出")
@RestController
@RequestMapping("/api/admin/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceDetailService deviceDetailService;
    private final DeviceExcelService deviceExcelService;

    @Operation(summary = "分页查询设备", description = "按企业、类型与状态分页返回设备")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<DeviceVO>> page(DeviceQueryDTO query) {
        return Result.ok(deviceService.page(query));
    }

    @Operation(summary = "设备详情", description = "按设备 ID 返回详情与最近趋势")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<DeviceDetailVO> detail(
            @Parameter(description = "设备 ID", required = true) @PathVariable Long id,
            @Parameter(description = "统计范围：day/week/month") @RequestParam(defaultValue = "week") String range) {
        return Result.ok(deviceDetailService.detail(id, range));
    }

    @Operation(summary = "设备趋势", description = "按时间范围返回指定设备的趋势数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/trend")
    public Result<List<MonitorTrendVO>> trend(
            @Parameter(description = "设备 ID", required = true) @PathVariable Long id,
            @Parameter(description = "统计范围：day/week/month") @RequestParam(defaultValue = "week") String range) {
        return Result.ok(deviceDetailService.trend(id, range));
    }

    @Operation(summary = "下载导入模板", description = "导出设备导入用的 Excel 模板")
    @ApiResponse(responseCode = "200", description = "下载成功")
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        deviceExcelService.exportTemplate(response);
    }

    @Operation(summary = "导出设备", description = "按当前筛选条件导出设备 Excel")
    @ApiResponse(responseCode = "200", description = "导出成功")
    @GetMapping("/export")
    public void export(DeviceQueryDTO query, HttpServletResponse response) throws IOException {
        deviceExcelService.export(query, response);
    }

    @Operation(summary = "导入设备", description = "解析 Excel 批量新增设备，需 device:import 权限")
    @ApiResponse(responseCode = "200", description = "导入完成，返回成功与失败明细")
    @PostMapping("/import")
    @SaCheckPermission("device:import")
    public Result<DeviceImportResultVO> importDevices(
            @Parameter(description = "设备导入 Excel 文件", required = true) @RequestParam("file") MultipartFile file) {
        return Result.ok(deviceExcelService.importDevices(file));
    }

    @Operation(summary = "新建设备", description = "需 device:edit 权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    @SaCheckPermission("device:edit")
    public Result<String> create(@RequestBody DeviceSaveDTO request) {
        return Result.ok(String.valueOf(deviceService.create(request)));
    }

    @Operation(summary = "修改设备", description = "需 device:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    @SaCheckPermission("device:edit")
    public Result<Void> update(@Parameter(description = "设备 ID", required = true) @PathVariable Long id,
                               @RequestBody DeviceSaveDTO request) {
        request.setId(id);
        deviceService.update(request);
        return Result.ok();
    }

    @Operation(summary = "删除设备", description = "需 device:edit 权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @SaCheckPermission("device:edit")
    public Result<Void> delete(@Parameter(description = "设备 ID", required = true) @PathVariable Long id) {
        deviceService.delete(id);
        return Result.ok();
    }
}
