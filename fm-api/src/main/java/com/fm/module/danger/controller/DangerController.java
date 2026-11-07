package com.fm.module.danger.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.danger.dto.DangerAssignDTO;
import com.fm.module.danger.dto.DangerAuditDTO;
import com.fm.module.danger.dto.DangerBatchAssignDTO;
import com.fm.module.danger.dto.DangerDealDTO;
import com.fm.module.danger.dto.DangerQueryDTO;
import com.fm.module.danger.dto.DangerSaveDTO;
import com.fm.module.danger.service.DangerService;
import com.fm.module.danger.vo.DangerBatchAssignResultVO;
import com.fm.module.danger.vo.DangerTimelineVO;
import com.fm.module.danger.vo.DangerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 隐患管理接口：跟踪、上报、指派、整改与审核。
 */
@Tag(name = "隐患管理", description = "隐患跟踪、上报、指派、整改与审核")
@RestController
@RequestMapping("/api/admin/danger")
@RequiredArgsConstructor
public class DangerController {

    private final DangerService dangerService;

    @Operation(summary = "分页查询隐患", description = "按企业、状态与类型分页返回隐患列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<DangerVO>> page(DangerQueryDTO query) {
        return Result.ok(dangerService.page(query));
    }

    @Operation(summary = "隐患详情", description = "按隐患 ID 返回详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<DangerVO> detail(@Parameter(description = "隐患 ID", required = true) @PathVariable Long id) {
        return Result.ok(dangerService.detail(id));
    }

    @Operation(summary = "隐患时间线", description = "按隐患 ID 返回上报、指派、整改与审核记录")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/timeline")
    public Result<List<DangerTimelineVO>> timeline(
            @Parameter(description = "隐患 ID", required = true) @PathVariable Long id) {
        return Result.ok(dangerService.timeline(id));
    }

    @Operation(summary = "上报隐患", description = "新隐患上报，返回隐患编号，需 danger:report 权限")
    @ApiResponse(responseCode = "200", description = "上报成功")
    @SaCheckPermission("danger:report")
    @PostMapping
    public Result<String> report(@Valid @RequestBody DangerSaveDTO request) {
        return Result.ok(String.valueOf(dangerService.report(request)));
    }

    @Operation(summary = "指派隐患", description = "把隐患指派给处理人，需 danger:assign 权限")
    @ApiResponse(responseCode = "200", description = "指派成功")
    @SaCheckPermission("danger:assign")
    @PutMapping("/{id}/assign")
    public Result<Void> assign(@Parameter(description = "隐患 ID", required = true) @PathVariable Long id,
                               @Valid @RequestBody DangerAssignDTO request) {
        dangerService.assign(id, request);
        return Result.ok();
    }

    @Operation(summary = "批量指派隐患", description = "逐条指派并返回成功数与失败明细，单次最多 100 条，需 danger:assign 权限")
    @ApiResponse(responseCode = "200", description = "批量指派完成")
    @SaCheckPermission("danger:assign")
    @PutMapping("/batch-assign")
    public Result<DangerBatchAssignResultVO> batchAssign(@Valid @RequestBody DangerBatchAssignDTO request) {
        return Result.ok(dangerService.batchAssign(request));
    }

    @Operation(summary = "整改隐患", description = "提交整改结果")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @PostMapping("/{id}/deal")
    public Result<Void> deal(@Parameter(description = "隐患 ID", required = true) @PathVariable Long id,
                             @Valid @RequestBody DangerDealDTO request) {
        dangerService.deal(id, request);
        return Result.ok();
    }

    @Operation(summary = "审核隐患", description = "审核整改结果并归档，需 danger:audit 权限")
    @ApiResponse(responseCode = "200", description = "审核成功")
    @SaCheckPermission("danger:audit")
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@Parameter(description = "隐患 ID", required = true) @PathVariable Long id,
                              @Valid @RequestBody DangerAuditDTO request) {
        dangerService.audit(id, request);
        return Result.ok();
    }
}
