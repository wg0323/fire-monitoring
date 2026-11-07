package com.fm.module.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.system.dto.IdsDTO;
import com.fm.module.system.dto.WorkAreaQueryDTO;
import com.fm.module.system.dto.WorkAreaSaveDTO;
import com.fm.module.system.service.SysWorkAreaService;
import com.fm.module.system.vo.UserVO;
import com.fm.module.system.vo.WorkAreaVO;
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
 * 工作区管理接口。
 */
@Tag(name = "工作区管理", description = "工作区的查询、维护与商户/人员绑定")
@RestController
@RequestMapping("/api/admin/system/work-area")
@RequiredArgsConstructor
public class SysWorkAreaController {

    private final SysWorkAreaService workAreaService;

    @Operation(summary = "分页查询工作区", description = "按名称与运营中心分页返回工作区")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<WorkAreaVO>> page(WorkAreaQueryDTO query) {
        return Result.ok(workAreaService.page(query));
    }

    @Operation(summary = "工作区下拉列表", description = "按租户返回可选工作区")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/options")
    public Result<List<WorkAreaVO>> options(
            @Parameter(description = "租户 ID，可为空") @RequestParam(required = false) Long tenantId) {
        return Result.ok(workAreaService.options(tenantId));
    }

    @Operation(summary = "工作区详情", description = "按工作区 ID 返回详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<WorkAreaVO> detail(@Parameter(description = "工作区 ID", required = true) @PathVariable Long id) {
        return Result.ok(workAreaService.detail(id));
    }

    @Operation(summary = "新建工作区", description = "需 work-area:edit 权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    @SaCheckPermission("work-area:edit")
    public Result<String> create(@RequestBody WorkAreaSaveDTO request) {
        return Result.ok(String.valueOf(workAreaService.create(request)));
    }

    @Operation(summary = "修改工作区", description = "需 work-area:edit 权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    @SaCheckPermission("work-area:edit")
    public Result<Void> update(@Parameter(description = "工作区 ID", required = true) @PathVariable Long id,
                               @RequestBody WorkAreaSaveDTO request) {
        workAreaService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除工作区", description = "需 work-area:edit 权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @SaCheckPermission("work-area:edit")
    public Result<Void> delete(@Parameter(description = "工作区 ID", required = true) @PathVariable Long id) {
        workAreaService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "绑定商户", description = "覆盖式保存工作区关联商户，需 work-area:edit 权限")
    @ApiResponse(responseCode = "200", description = "绑定成功")
    @PutMapping("/{id}/merchants")
    @SaCheckPermission("work-area:edit")
    public Result<Void> bindMerchants(@Parameter(description = "工作区 ID", required = true) @PathVariable Long id,
                                      @RequestBody IdsDTO request) {
        workAreaService.bindMerchants(id, request.getIds());
        return Result.ok();
    }

    @Operation(summary = "绑定人员", description = "覆盖式保存工作区关联人员，需 work-area:edit 权限")
    @ApiResponse(responseCode = "200", description = "绑定成功")
    @PutMapping("/{id}/users")
    @SaCheckPermission("work-area:edit")
    public Result<Void> bindUsers(@Parameter(description = "工作区 ID", required = true) @PathVariable Long id,
                                  @RequestBody IdsDTO request) {
        workAreaService.bindUsers(id, request.getIds());
        return Result.ok();
    }

    @Operation(summary = "工作区人员列表", description = "返回工作区已绑定人员")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/users")
    public Result<List<UserVO>> users(@Parameter(description = "工作区 ID", required = true) @PathVariable Long id) {
        return Result.ok(workAreaService.users(id));
    }

    @Operation(summary = "工作区商户列表", description = "返回工作区已绑定商户 ID 列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/merchants")
    public Result<List<String>> merchants(
            @Parameter(description = "工作区 ID", required = true) @PathVariable Long id) {
        return Result.ok(workAreaService.merchantIds(id).stream()
                .map(String::valueOf).collect(Collectors.toList()));
    }
}
