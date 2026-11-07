package com.fm.module.notification.controller;

import com.fm.common.api.Result;
import com.fm.module.notification.config.CallProperties;
import com.fm.module.notification.converter.CallConverter;
import com.fm.module.notification.dto.CallConfigDTO;
import com.fm.module.notification.entity.SevenMoorConfig;
import com.fm.module.notification.service.CallConfigService;
import com.fm.module.notification.vo.CallConfigVO;
import com.fm.module.system.support.AdminGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 租户外呼配置接口：保存后立即生效，无需重启。
 */
@Tag(name = "外呼配置", description = "租户外呼账号配置，保存后立即生效")
@RestController
@RequestMapping("/api/admin/call-config")
@RequiredArgsConstructor
public class CallConfigController {

    private final CallConfigService callConfigService;
    private final CallProperties callProperties;

    @Operation(summary = "查询外呼配置", description = "返回当前租户的外呼账号配置，密钥不回显")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public Result<CallConfigVO> current() {
        SevenMoorConfig config = callConfigService.current();
        CallConfigVO vo = CallConverter.toVO(config);
        vo.setMockMode(callProperties.isMock());
        return Result.ok(vo);
    }

    @Operation(summary = "保存外呼配置", description = "保存后立即生效，无需重启，仅平台/租户管理员可操作")
    @ApiResponse(responseCode = "200", description = "保存成功")
    @PutMapping
    public Result<Void> save(@Valid @RequestBody CallConfigDTO request) {
        // MenuBaseline 权限基线只有 alarm:call，不存在 call:config；注解式 @SaCheckPermission
        // 会让所有非通配权限的管理员保存外呼配置时 403。外呼账号配置属于租户级管理写操作，
        // 沿用系统管理模块统一的 AdminGuard.requireAdmin()：仅平台管理员 / 租户管理员可写，
        // 仍是显式权限约束（且 SaInterceptor 已保证登录态），不是无鉴权裸接口。
        AdminGuard.requireAdmin();
        SevenMoorConfig config = new SevenMoorConfig();
        config.setAccountId(request.getAccountId());
        config.setApiSecret(request.getApiSecret());
        config.setHost(request.getHost());
        config.setFromExten(request.getFromExten());
        callConfigService.save(config);
        return Result.ok();
    }
}
