package com.fm.module.system.controller;

import com.fm.common.api.Result;
import com.fm.module.system.dto.DeptSaveDTO;
import com.fm.module.system.service.SysDeptService;
import com.fm.module.system.vo.DeptVO;
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
 * 部门管理接口。
 */
@Tag(name = "部门管理", description = "部门树的查询与维护")
@RestController
@RequestMapping("/api/admin/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @Operation(summary = "部门树", description = "按租户返回部门树，tenantId 为空时取当前租户")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/tree")
    public Result<List<DeptVO>> tree(
            @Parameter(description = "租户 ID，可为空") @RequestParam(required = false) Long tenantId) {
        return Result.ok(deptService.tree(tenantId));
    }

    @Operation(summary = "新建部门", description = "保存部门并返回主键")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<String> create(@RequestBody DeptSaveDTO request) {
        return Result.ok(String.valueOf(deptService.create(request)));
    }

    @Operation(summary = "修改部门", description = "按主键更新部门")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "部门 ID", required = true) @PathVariable Long id,
                               @RequestBody DeptSaveDTO request) {
        deptService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除部门", description = "按主键删除部门")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "部门 ID", required = true) @PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
