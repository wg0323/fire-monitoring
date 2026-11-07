package com.fm.module.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fm.common.api.Result;
import com.fm.module.device.converter.DictConverter;
import com.fm.module.device.dto.DictItemDTO;
import com.fm.module.device.service.DictService;
import com.fm.module.device.vo.DeviceTypeVO;
import com.fm.module.device.vo.DictItemVO;
import com.fm.module.device.vo.DictVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典查询接口。
 */
@Tag(name = "字典管理", description = "设备类型、厂商、报警/隐患类型与行政区划字典")
@RestController
@RequestMapping("/api/admin/device/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @Operation(summary = "设备类型字典", description = "返回设备类型选项")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/device-type")
    public Result<List<DeviceTypeVO>> deviceTypes() {
        return Result.ok(DictConverter.toDeviceTypeVOList(dictService.deviceTypes()));
    }

    @Operation(summary = "设备厂商字典", description = "返回设备厂商选项")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/device-maker")
    public Result<List<DictVO>> deviceMakers() {
        return Result.ok(dictService.deviceMakers().stream()
                .map(DictConverter::toVO).collect(Collectors.toList()));
    }

    @Operation(summary = "报警类型字典", description = "返回报警类型选项")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/alarm-type")
    public Result<List<DictVO>> alarmTypes() {
        return Result.ok(dictService.alarmTypes().stream()
                .map(DictConverter::toVO).collect(Collectors.toList()));
    }

    @Operation(summary = "隐患类型字典", description = "返回隐患类型选项")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/danger-type")
    public Result<List<DictVO>> dangerTypes() {
        return Result.ok(dictService.dangerTypes().stream()
                .map(DictConverter::toVO).collect(Collectors.toList()));
    }

    @Operation(summary = "企业类型字典", description = "返回企业类型选项")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/merchant-type")
    public Result<List<DictVO>> merchantTypes() {
        return Result.ok(dictService.merchantTypes().stream()
                .map(DictConverter::toVO).collect(Collectors.toList()));
    }

    @Operation(summary = "行政区划字典", description = "按关键字返回行政区划选项")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/adcode")
    public Result<List<DictVO>> adcodes(
            @Parameter(description = "区划名称关键字，可为空") @RequestParam(required = false) String keyword) {
        return Result.ok(dictService.adcodes(keyword).stream()
                .map(DictConverter::toVO).collect(Collectors.toList()));
    }

    @Operation(summary = "字典项管理列表", description = "按字典类型返回可维护的字典项")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/manage/{type}")
    public Result<List<DictItemVO>> manageList(
            @Parameter(description = "字典类型", required = true) @PathVariable String type) {
        return Result.ok(dictService.adminList(type));
    }

    @Operation(summary = "新建字典项", description = "需 dict:edit 权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping("/{type}")
    @SaCheckPermission("dict:edit")
    public Result<String> create(@Parameter(description = "字典类型", required = true) @PathVariable String type,
                                 @RequestBody DictItemDTO request) {
        return Result.ok(String.valueOf(dictService.saveItem(type, request)));
    }

    @Operation(summary = "修改字典项", description = "需 dict:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{type}/{id}")
    @SaCheckPermission("dict:edit")
    public Result<String> update(@Parameter(description = "字典类型", required = true) @PathVariable String type,
                                 @Parameter(description = "字典项 ID", required = true) @PathVariable Long id,
                                 @RequestBody DictItemDTO request) {
        request.setId(id);
        return Result.ok(String.valueOf(dictService.saveItem(type, request)));
    }

    @Operation(summary = "删除字典项", description = "需 dict:edit 权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{type}/{id}")
    @SaCheckPermission("dict:edit")
    public Result<Void> delete(@Parameter(description = "字典类型", required = true) @PathVariable String type,
                               @Parameter(description = "字典项 ID", required = true) @PathVariable Long id) {
        dictService.deleteItem(type, id);
        return Result.ok();
    }
}
