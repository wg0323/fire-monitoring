package com.fm.module.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fm.common.api.Result;
import com.fm.module.device.dto.BuildingSaveDTO;
import com.fm.module.device.service.MerchantBuildingService;
import com.fm.module.device.vo.BuildingVO;
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

import java.util.List;

/**
 * 商户楼宇接口。
 */
@Tag(name = "商户楼宇", description = "企业下属楼宇的查询与维护")
@RestController
@RequestMapping("/api/admin/building")
@RequiredArgsConstructor
public class MerchantBuildingController {

    private final MerchantBuildingService buildingService;

    @Operation(summary = "企业楼宇列表", description = "按企业 ID 返回楼宇列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/list")
    public Result<List<BuildingVO>> list(
            @Parameter(description = "企业 ID", required = true) @RequestParam Long merchantId) {
        return Result.ok(buildingService.listByMerchant(merchantId));
    }

    @Operation(summary = "新建楼宇", description = "需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    @SaCheckPermission("merchant:edit")
    public Result<String> create(@RequestBody BuildingSaveDTO request) {
        return Result.ok(String.valueOf(buildingService.create(request)));
    }

    @Operation(summary = "修改楼宇", description = "需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    @SaCheckPermission("merchant:edit")
    public Result<Void> update(@Parameter(description = "楼宇 ID", required = true) @PathVariable Long id,
                               @RequestBody BuildingSaveDTO request) {
        request.setId(id);
        buildingService.update(request);
        return Result.ok();
    }

    @Operation(summary = "删除楼宇", description = "需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @SaCheckPermission("merchant:edit")
    public Result<Void> delete(@Parameter(description = "楼宇 ID", required = true) @PathVariable Long id) {
        buildingService.delete(id);
        return Result.ok();
    }
}
