package com.fm.module.system.controller;

import com.fm.common.api.Result;
import com.fm.module.system.dto.LoginDTO;
import com.fm.module.system.dto.PasswordChangeDTO;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.service.AuthService;
import com.fm.module.system.vo.LoginVO;
import com.fm.module.system.vo.MenuVO;
import com.fm.module.system.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 后台认证接口：仅平台管理员、租户管理员、租户值守可登录。
 */
@Tag(name = "后台认证", description = "后台登录、登出、改密与本人信息查询")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Set<String> BACKEND_ROLES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    SysRole.PLATFORM_ADMIN, SysRole.TENANT_ADMIN, SysRole.TENANT_DUTY)));

    private final AuthService authService;

    @Operation(summary = "后台登录", description = "仅平台管理员、租户管理员、租户值守可登录，返回 token 与用户信息")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO request, HttpServletRequest servletRequest) {
        return Result.ok(authService.login(request, servletRequest.getRemoteAddr(), "pc", BACKEND_ROLES));
    }

    @Operation(summary = "登出", description = "注销当前会话")
    @ApiResponse(responseCode = "200", description = "登出成功")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "修改密码", description = "校验原密码后重设新密码")
    @ApiResponse(responseCode = "200", description = "修改成功")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeDTO request) {
        authService.changePassword(request);
        return Result.ok();
    }

    @Operation(summary = "当前用户信息", description = "返回登录用户的基础信息与角色")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(authService.info());
    }

    @Operation(summary = "当前用户菜单", description = "按角色权限返回可见菜单树")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/menus")
    public Result<List<MenuVO>> menus() {
        return Result.ok(authService.menus());
    }
}
