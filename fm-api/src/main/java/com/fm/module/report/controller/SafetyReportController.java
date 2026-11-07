package com.fm.module.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.report.dto.SafetyReportQueryDTO;
import com.fm.module.report.dto.SafetyReportSaveDTO;
import com.fm.module.report.service.SafetyReportService;
import com.fm.module.report.vo.SafetyReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 安全报告接口。
 */
@Tag(name = "安全报告", description = "安全报告的分页查询、新建与删除")
@RestController
@RequestMapping("/api/admin/safety-report")
@RequiredArgsConstructor
public class SafetyReportController {

    private final SafetyReportService safetyReportService;

    @Operation(summary = "分页查询安全报告", description = "按企业、报告类型与时间范围分页返回")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<SafetyReportVO>> page(SafetyReportQueryDTO query) {
        return Result.ok(safetyReportService.page(query));
    }

    @Operation(summary = "新建安全报告", description = "保存月度/年度报告并返回主键")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SafetyReportSaveDTO request) {
        return Result.ok(safetyReportService.create(request));
    }

    @Operation(summary = "删除安全报告", description = "按主键删除，不存在时忽略")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "报告 ID", required = true) @PathVariable Long id) {
        safetyReportService.delete(id);
        return Result.ok();
    }
}
