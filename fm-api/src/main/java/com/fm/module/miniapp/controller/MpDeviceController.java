package com.fm.module.miniapp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.common.exception.BizException;
import com.fm.module.device.converter.DeviceConverter;
import com.fm.module.device.converter.DictConverter;
import com.fm.module.device.dto.DeviceQueryDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.service.DeviceService;
import com.fm.module.device.service.DictService;
import com.fm.module.device.service.MerchantService;
import com.fm.module.device.vo.DeviceVO;
import com.fm.module.device.vo.DictVO;
import com.fm.module.device.vo.MerchantOptionVO;
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
import java.util.stream.Collectors;

/**
 * 小程序设备接口：商户仅能看到本商户设备，外勤限所属工作区商户。
 */
@Tag(name = "小程序设备", description = "商户仅能看到本商户设备，外勤限所属工作区商户")
@RestController
@RequestMapping("/api/mp/device")
@RequiredArgsConstructor
public class MpDeviceController {

    private final DeviceService deviceService;
    private final DictService dictService;
    private final MerchantService merchantService;

    @Operation(summary = "分页查询设备", description = "按当前登录账号的角色收窄可见设备")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<DeviceVO>> page(DeviceQueryDTO query) {
        return Result.ok(deviceService.page(query));
    }

    @Operation(summary = "设备详情", description = "按设备 ID 返回详情，不存在时返回业务异常")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<DeviceVO> detail(@Parameter(description = "设备 ID", required = true) @PathVariable Long id) {
        Device device = deviceService.getById(id);
        if (device == null) {
            throw new BizException("设备不存在");
        }
        return Result.ok(DeviceConverter.toVO(device));
    }

    @Operation(summary = "企业下拉列表", description = "最多返回 50 条，按角色收窄可见企业")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/merchant-options")
    public Result<List<MerchantOptionVO>> merchantOptions(
            @Parameter(description = "企业名称关键字，可为空") @RequestParam(required = false) String keyword) {
        return Result.ok(merchantService.merchantOptions(keyword));
    }

    @Operation(summary = "隐患类型字典", description = "返回隐患类型选项")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/dict/danger-type")
    public Result<List<DictVO>> dangerTypes() {
        return Result.ok(dictService.dangerTypes().stream()
                .map(DictConverter::toVO)
                .collect(Collectors.toList()));
    }
}
