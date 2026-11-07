package com.fm.module.miniapp.controller;

import com.fm.common.api.Result;
import com.fm.module.miniapp.dto.PhoneLoginDTO;
import com.fm.module.miniapp.dto.SwitchMerchantDTO;
import com.fm.module.miniapp.dto.WxLoginDTO;
import com.fm.module.miniapp.service.WxMiniappService;
import com.fm.module.system.service.AuthService;
import com.fm.module.system.vo.LoginVO;
import com.fm.module.system.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 小程序认证接口：手机号白名单登录。
 */
@Tag(name = "小程序认证", description = "小程序微信登录、手机号登录与本人信息")
@RestController
@RequestMapping("/api/mp/auth")
@RequiredArgsConstructor
public class MpAuthController {

    private final AuthService authService;
    private final WxMiniappService wxMiniappService;

    @Operation(summary = "手机号登录", description = "手机号白名单校验通过后建立会话")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @PostMapping("/phone-login")
    public Result<LoginVO> phoneLogin(@Valid @RequestBody PhoneLoginDTO request,
                                      HttpServletRequest servletRequest) {
        return Result.ok(wxMiniappService.phoneLogin(request, servletRequest.getRemoteAddr()));
    }

    @Operation(summary = "微信登录", description = "用微信登录 code 换取会话")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @PostMapping("/wx-login")
    public Result<LoginVO> wxLogin(@Valid @RequestBody WxLoginDTO request, HttpServletRequest servletRequest) {
        return Result.ok(wxMiniappService.login(request.getCode(), servletRequest.getRemoteAddr()));
    }

    @Operation(summary = "小程序登出", description = "注销当前会话")
    @ApiResponse(responseCode = "200", description = "登出成功")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "切换门店", description = "在本人关联的商户之间切换，仅多门店账号需要调用")
    @ApiResponse(responseCode = "200", description = "切换成功")
    @PostMapping("/switch-merchant")
    public Result<Void> switchMerchant(@Valid @RequestBody SwitchMerchantDTO request) {
        authService.switchMerchant(request.getMerchantId());
        return Result.ok();
    }

    @Operation(summary = "当前用户信息", description = "返回小程序登录用户的基础信息")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(authService.info());
    }
}
