package com.fm.module.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fm.common.api.Result;
import com.fm.module.system.converter.MenuConverter;
import com.fm.module.system.dto.MenuSaveDTO;
import com.fm.module.system.service.SysMenuService;
import com.fm.module.system.vo.MenuVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理接口：平台写 tenant 0，租户管理员写本租户。
 */
@Tag(name = "菜单管理", description = "菜单树的查询与维护，平台写 tenant 0，租户管理员写本租户")
@RestController
@RequestMapping("/api/admin/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "菜单树", description = "返回当前账号可见的菜单树")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/tree")
    public Result<List<MenuVO>> tree() {
        return Result.ok(MenuConverter.toTree(menuService.listTreeNodes()));
    }

    @Operation(summary = "新建菜单", description = "需 menu:edit 权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    @SaCheckPermission("menu:edit")
    public Result<String> create(@RequestBody MenuSaveDTO request) {
        return Result.ok(String.valueOf(menuService.create(request)));
    }

    @Operation(summary = "修改菜单", description = "需 menu:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    @SaCheckPermission("menu:edit")
    public Result<Void> update(@Parameter(description = "菜单 ID", required = true) @PathVariable Long id,
                               @RequestBody MenuSaveDTO request) {
        menuService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除菜单", description = "需 menu:edit 权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @SaCheckPermission("menu:edit")
    public Result<Void> delete(@Parameter(description = "菜单 ID", required = true) @PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
