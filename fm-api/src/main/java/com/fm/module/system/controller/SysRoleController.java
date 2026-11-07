package com.fm.module.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.system.dto.IdsDTO;
import com.fm.module.system.dto.RoleQueryDTO;
import com.fm.module.system.dto.RoleSaveDTO;
import com.fm.module.system.service.SysRoleService;
import com.fm.module.system.vo.RoleVO;
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
import java.util.stream.Collectors;

/**
 * 角色管理接口。
 */
@Tag(name = "角色管理", description = "角色维护与菜单授权")
@RestController
@RequestMapping("/api/admin/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "分页查询角色", description = "需 role:grant 权限")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    @SaCheckPermission("role:grant")
    public Result<Page<RoleVO>> page(RoleQueryDTO query) {
        return Result.ok(roleService.page(query));
    }

    @Operation(summary = "角色下拉列表", description = "按租户与用户类型返回可选角色")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/list")
    public Result<List<RoleVO>> list(
            @Parameter(description = "租户 ID，可为空") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "用户类型，可为空") @RequestParam(required = false) String userType) {
        return Result.ok(roleService.list(tenantId, userType));
    }

    @Operation(summary = "新建角色", description = "需 role:edit 权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    @SaCheckPermission("role:edit")
    public Result<String> create(@RequestBody RoleSaveDTO request) {
        return Result.ok(String.valueOf(roleService.create(request)));
    }

    @Operation(summary = "修改角色", description = "需 role:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    @SaCheckPermission("role:edit")
    public Result<Void> update(@Parameter(description = "角色 ID", required = true) @PathVariable Long id,
                               @RequestBody RoleSaveDTO request) {
        roleService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除角色", description = "需 role:edit 权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @SaCheckPermission("role:edit")
    public Result<Void> delete(@Parameter(description = "角色 ID", required = true) @PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "角色已授权菜单", description = "返回角色已勾选的菜单 ID 列表，需 role:grant 权限")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/menus")
    @SaCheckPermission("role:grant")
    public Result<List<String>> menus(@Parameter(description = "角色 ID", required = true) @PathVariable Long id) {
        return Result.ok(roleService.menuIds(id).stream()
                .map(String::valueOf).collect(Collectors.toList()));
    }

    @Operation(summary = "角色授权菜单", description = "覆盖式保存角色菜单授权，需 role:grant 权限")
    @ApiResponse(responseCode = "200", description = "授权成功")
    @PutMapping("/{id}/menus")
    @SaCheckPermission("role:grant")
    public Result<Void> assignMenus(@Parameter(description = "角色 ID", required = true) @PathVariable Long id,
                                    @RequestBody IdsDTO request) {
        roleService.assignMenus(id, request.getIds());
        return Result.ok();
    }
}
