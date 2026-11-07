package com.fm.module.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fm.common.api.Result;
import com.fm.module.device.converter.DeviceConverter;
import com.fm.module.device.converter.FloorConverter;
import com.fm.module.device.dto.FloorPlanDTO;
import com.fm.module.device.dto.FloorPointsSaveDTO;
import com.fm.module.device.dto.FloorSaveDTO;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.service.DeviceService;
import com.fm.module.device.service.MerchantFloorService;
import com.fm.module.device.vo.DevicePointVO;
import com.fm.module.device.vo.FloorVO;
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

import java.util.Collections;
import java.util.List;

/**
 * 商户楼层接口。
 */
@Tag(name = "商户楼层", description = "楼层查询、楼层平面图与点位布置")
@RestController
@RequestMapping("/api/admin/floor")
@RequiredArgsConstructor
public class MerchantFloorController {

    private final MerchantFloorService floorService;
    private final DeviceService deviceService;

    @Operation(summary = "楼层列表", description = "按楼宇或企业返回楼层列表，两者都为空时返回空集合")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/list")
    public Result<List<FloorVO>> list(
            @Parameter(description = "企业 ID，可为空") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "楼宇 ID，可为空") @RequestParam(required = false) Long buildingId) {
        List<MerchantFloor> floors;
        if (buildingId != null) {
            floors = floorService.listByBuilding(buildingId);
        } else if (merchantId != null) {
            floors = floorService.listByMerchant(merchantId);
        } else {
            floors = Collections.emptyList();
        }
        return Result.ok(FloorConverter.toVOList(floors));
    }

    @Operation(summary = "新建楼层", description = "需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    @SaCheckPermission("merchant:edit")
    public Result<String> create(@RequestBody FloorSaveDTO request) {
        return Result.ok(String.valueOf(floorService.create(request)));
    }

    @Operation(summary = "修改楼层", description = "需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    @SaCheckPermission("merchant:edit")
    public Result<Void> update(@Parameter(description = "楼层 ID", required = true) @PathVariable Long id,
                               @RequestBody FloorSaveDTO request) {
        request.setId(id);
        floorService.update(request);
        return Result.ok();
    }

    @Operation(summary = "删除楼层", description = "需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @SaCheckPermission("merchant:edit")
    public Result<Void> delete(@Parameter(description = "楼层 ID", required = true) @PathVariable Long id) {
        floorService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "保存楼层平面图", description = "保存平面图地址与尺寸，需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "保存成功")
    @PutMapping("/{id}/plan")
    @SaCheckPermission("merchant:edit")
    public Result<Void> updatePlan(@Parameter(description = "楼层 ID", required = true) @PathVariable Long id,
                                   @RequestBody FloorPlanDTO request) {
        floorService.updatePlan(id, request);
        return Result.ok();
    }

    @Operation(summary = "楼层已布置点位", description = "返回楼层平面图上已布置的设备点位")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/points")
    public Result<List<DevicePointVO>> points(
            @Parameter(description = "楼层 ID", required = true) @PathVariable Long id) {
        return Result.ok(DeviceConverter.toPointVOList(deviceService.floorPoints(id)));
    }

    @Operation(summary = "楼层可布置设备", description = "返回该楼层可供布置的设备候选")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/candidates")
    public Result<List<DevicePointVO>> candidates(
            @Parameter(description = "楼层 ID", required = true) @PathVariable Long id) {
        return Result.ok(DeviceConverter.toPointVOList(deviceService.floorCandidates(id)));
    }

    @Operation(summary = "保存楼层点位", description = "覆盖式保存楼层设备点位，需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "保存成功")
    @PutMapping("/{id}/points")
    @SaCheckPermission("merchant:edit")
    public Result<Void> savePoints(@Parameter(description = "楼层 ID", required = true) @PathVariable Long id,
                                   @RequestBody FloorPointsSaveDTO request) {
        deviceService.saveFloorPoints(id, request);
        return Result.ok();
    }

    /**
     * 兼容旧路径，后续版本删除。
     */
    @Operation(summary = "楼层点位（旧路径）", description = "兼容旧路径，新代码请用 /{id}/points，后续版本删除")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/devices")
    public Result<List<DevicePointVO>> devices(
            @Parameter(description = "楼层 ID", required = true) @PathVariable Long id) {
        return Result.ok(DeviceConverter.toPointVOList(deviceService.floorPoints(id)));
    }
}
