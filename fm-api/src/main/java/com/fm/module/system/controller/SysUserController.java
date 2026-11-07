package com.fm.module.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.system.dto.IdsDTO;
import com.fm.module.system.dto.PasswordResetDTO;
import com.fm.module.system.dto.UserQueryDTO;
import com.fm.module.system.dto.UserSaveDTO;
import com.fm.module.system.service.SysUserService;
import com.fm.module.system.vo.UserVO;
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
 * 用户管理接口。
 */
@Tag(name = "用户管理", description = "后台用户的查询、维护与工作区绑定")
@RestController
@RequestMapping("/api/admin/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "分页查询用户", description = "按姓名、角色与状态分页返回用户，需 user:edit 权限")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    @SaCheckPermission("user:edit")
    public Result<Page<UserVO>> page(UserQueryDTO query) {
        return Result.ok(userService.page(query));
    }

    @Operation(summary = "用户详情", description = "按用户 ID 返回详情，需 user:edit 权限")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    @SaCheckPermission("user:edit")
    public Result<UserVO> detail(@Parameter(description = "用户 ID", required = true) @PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @Operation(summary = "用户下拉列表", description = "按租户与用户类型返回可选用户，需 user:edit 权限")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/options")
    @SaCheckPermission("user:edit")
    public Result<List<UserVO>> options(
            @Parameter(description = "租户 ID，可为空") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "用户类型，可为空") @RequestParam(required = false) String userType) {
        return Result.ok(userService.options(tenantId, userType));
    }

    @Operation(summary = "新建用户", description = "需 user:edit 权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    @SaCheckPermission("user:edit")
    public Result<String> create(@RequestBody UserSaveDTO request) {
        return Result.ok(String.valueOf(userService.create(request)));
    }

    @Operation(summary = "修改用户", description = "需 user:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    @SaCheckPermission("user:edit")
    public Result<Void> update(@Parameter(description = "用户 ID", required = true) @PathVariable Long id,
                               @RequestBody UserSaveDTO request) {
        userService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除用户", description = "需 user:edit 权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @SaCheckPermission("user:edit")
    public Result<Void> delete(@Parameter(description = "用户 ID", required = true) @PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "绑定工作区", description = "覆盖式保存用户可访问的工作区，需 user:bind 权限")
    @ApiResponse(responseCode = "200", description = "绑定成功")
    @PutMapping("/{id}/work-areas")
    @SaCheckPermission("user:bind")
    public Result<Void> bindWorkAreas(@Parameter(description = "用户 ID", required = true) @PathVariable Long id,
                                      @RequestBody IdsDTO request) {
        userService.bindWorkAreas(id, request.getIds());
        return Result.ok();
    }

    @Operation(summary = "重置密码", description = "管理员重置指定用户密码，需 user:edit 权限")
    @ApiResponse(responseCode = "200", description = "重置成功")
    @PutMapping("/{id}/password")
    @SaCheckPermission("user:edit")
    public Result<Void> resetPassword(@Parameter(description = "用户 ID", required = true) @PathVariable Long id,
                                      @RequestBody PasswordResetDTO request) {
        userService.resetPassword(id, request.getPassword());
        return Result.ok();
    }
}
