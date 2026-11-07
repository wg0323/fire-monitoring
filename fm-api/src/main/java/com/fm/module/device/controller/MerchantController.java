package com.fm.module.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.device.converter.MerchantConverter;
import com.fm.module.device.dto.MerchantDeviceBindDTO;
import com.fm.module.device.dto.MerchantQueryDTO;
import com.fm.module.device.dto.MerchantSaveDTO;
import com.fm.module.device.dto.MerchantStatusDTO;
import com.fm.module.device.dto.MerchantWorkAreaBindDTO;
import com.fm.module.device.service.MerchantService;
import com.fm.module.device.vo.MerchantDetailVO;
import com.fm.module.device.vo.MerchantOptionVO;
import com.fm.module.device.vo.MerchantVO;
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
 * 商户管理接口。
 */
@Tag(name = "企业管理", description = "企业（商户）的查询、维护与工作区/设备绑定")
@RestController
@RequestMapping("/api/admin/device/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "分页查询企业", description = "按名称、类型与状态分页返回企业")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<MerchantVO>> page(MerchantQueryDTO query) {
        return Result.ok(merchantService.page(query));
    }

    @Operation(summary = "企业下拉列表", description = "按租户返回可选企业")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/options")
    public Result<List<MerchantOptionVO>> options(
            @Parameter(description = "租户 ID，可为空") @RequestParam(required = false) Long tenantId) {
        return Result.ok(MerchantConverter.toOptionVOList(merchantService.options(tenantId)));
    }

    @Operation(summary = "企业详情", description = "按企业 ID 返回详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<MerchantDetailVO> detail(
            @Parameter(description = "企业 ID", required = true) @PathVariable Long id) {
        return Result.ok(merchantService.detail(id));
    }

    @Operation(summary = "新建企业", description = "需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    @SaCheckPermission("merchant:edit")
    public Result<String> create(@RequestBody MerchantSaveDTO request) {
        return Result.ok(String.valueOf(merchantService.create(request)));
    }

    @Operation(summary = "修改企业", description = "需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    @SaCheckPermission("merchant:edit")
    public Result<Void> update(@Parameter(description = "企业 ID", required = true) @PathVariable Long id,
                               @RequestBody MerchantSaveDTO request) {
        request.setId(id);
        merchantService.update(request);
        return Result.ok();
    }

    @Operation(summary = "删除企业", description = "需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @SaCheckPermission("merchant:edit")
    public Result<Void> delete(@Parameter(description = "企业 ID", required = true) @PathVariable Long id) {
        merchantService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "更新企业状态", description = "启用/停用企业，需 merchant:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}/status")
    @SaCheckPermission("merchant:edit")
    public Result<Void> updateStatus(@Parameter(description = "企业 ID", required = true) @PathVariable Long id,
                                     @RequestBody MerchantStatusDTO request) {
        merchantService.updateStatus(id, request.getStatus());
        return Result.ok();
    }

    @Operation(summary = "绑定工作区", description = "覆盖式保存企业关联工作区，需 merchant:bind 权限")
    @ApiResponse(responseCode = "200", description = "绑定成功")
    @PutMapping("/{id}/work-areas")
    @SaCheckPermission("merchant:bind")
    public Result<Void> bindWorkAreas(@Parameter(description = "企业 ID", required = true) @PathVariable Long id,
                                      @RequestBody MerchantWorkAreaBindDTO request) {
        merchantService.bindWorkAreas(id, request.getWorkAreaIds());
        return Result.ok();
    }

    @Operation(summary = "绑定设备", description = "把设备绑定到企业，需 merchant:bind 权限")
    @ApiResponse(responseCode = "200", description = "绑定成功")
    @PostMapping("/{id}/devices")
    @SaCheckPermission("merchant:bind")
    public Result<Void> bindDevices(@Parameter(description = "企业 ID", required = true) @PathVariable Long id,
                                    @RequestBody MerchantDeviceBindDTO request) {
        merchantService.bindDevices(id, request.getDeviceIds());
        return Result.ok();
    }

    @Operation(summary = "解绑设备", description = "解除企业与设备的绑定关系，需 merchant:bind 权限")
    @ApiResponse(responseCode = "200", description = "解绑成功")
    @DeleteMapping("/{id}/devices/{deviceId}")
    @SaCheckPermission("merchant:bind")
    public Result<Void> unbindDevice(@Parameter(description = "企业 ID", required = true) @PathVariable Long id,
                                     @Parameter(description = "设备 ID", required = true) @PathVariable Long deviceId) {
        merchantService.unbindDevice(id, deviceId);
        return Result.ok();
    }
}
