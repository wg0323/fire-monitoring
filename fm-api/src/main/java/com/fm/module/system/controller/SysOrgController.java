package com.fm.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.system.dto.OrgQueryDTO;
import com.fm.module.system.dto.OrgSaveDTO;
import com.fm.module.system.service.SysOrgService;
import com.fm.module.system.vo.OrgVO;
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
 * 运营中心（租户）管理接口，仅平台管理员可用。
 */
@Tag(name = "运营中心管理", description = "运营中心（租户）的查询与维护，仅平台管理员可用")
@RestController
@RequestMapping("/api/admin/system/tenant")
@RequiredArgsConstructor
public class SysOrgController {

    private final SysOrgService orgService;

    @Operation(summary = "分页查询运营中心", description = "按名称与状态分页返回运营中心")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<OrgVO>> page(OrgQueryDTO query) {
        return Result.ok(orgService.page(query));
    }

    @Operation(summary = "运营中心下拉列表", description = "返回可选的运营中心列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/list")
    public Result<List<OrgVO>> list() {
        return Result.ok(orgService.options());
    }

    @Operation(summary = "运营中心详情", description = "按主键返回运营中心详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<OrgVO> detail(@Parameter(description = "运营中心 ID", required = true) @PathVariable Long id) {
        return Result.ok(orgService.detail(id));
    }

    @Operation(summary = "新建运营中心", description = "保存运营中心并返回主键")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<String> create(@RequestBody OrgSaveDTO request) {
        return Result.ok(String.valueOf(orgService.create(request)));
    }

    @Operation(summary = "修改运营中心", description = "按主键更新运营中心")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "运营中心 ID", required = true) @PathVariable Long id,
                               @RequestBody OrgSaveDTO request) {
        orgService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除运营中心", description = "按主键删除运营中心")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "运营中心 ID", required = true) @PathVariable Long id) {
        orgService.delete(id);
        return Result.ok();
    }
}
