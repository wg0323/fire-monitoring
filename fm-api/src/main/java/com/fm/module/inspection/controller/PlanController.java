package com.fm.module.inspection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.inspection.dto.PlanMerchantQueryDTO;
import com.fm.module.inspection.dto.PlanQueryDTO;
import com.fm.module.inspection.dto.PlanSaveDTO;
import com.fm.module.inspection.service.PlanService;
import com.fm.module.inspection.vo.PlanMerchantVO;
import com.fm.module.inspection.vo.PlanResultVO;
import com.fm.module.inspection.vo.PlanVO;
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

import javax.validation.Valid;
import java.util.List;

/**
 * 巡检管理接口：计划按工作区创建，详情只展示该工作区商户。
 */
@Tag(name = "巡检管理", description = "巡检计划按工作区创建，详情只展示该工作区商户")
@RestController
@RequestMapping("/api/admin/inspection")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @Operation(summary = "分页查询巡检计划", description = "按工作区、状态与时间范围分页返回计划")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/plan/page")
    public Result<Page<PlanVO>> page(PlanQueryDTO query) {
        return Result.ok(planService.page(query));
    }

    @Operation(summary = "巡检计划详情", description = "按计划 ID 返回详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/plan/{id}")
    public Result<PlanVO> detail(@Parameter(description = "计划 ID", required = true) @PathVariable Long id) {
        return Result.ok(planService.detail(id));
    }

    @Operation(summary = "新建巡检计划", description = "按工作区创建计划并返回主键")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping("/plan")
    public Result<String> create(@Valid @RequestBody PlanSaveDTO request) {
        return Result.ok(String.valueOf(planService.create(request)));
    }

    @Operation(summary = "修改巡检计划", description = "按主键更新计划内容")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/plan")
    public Result<Void> update(@Valid @RequestBody PlanSaveDTO request) {
        planService.update(request);
        return Result.ok();
    }

    @Operation(summary = "删除巡检计划", description = "按主键删除计划")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/plan/{id}")
    public Result<Void> delete(@Parameter(description = "计划 ID", required = true) @PathVariable Long id) {
        planService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "计划关联商户", description = "返回该计划覆盖的商户列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/plan/{id}/merchants")
    public Result<List<PlanMerchantVO>> merchants(
            @Parameter(description = "计划 ID", required = true) @PathVariable Long id) {
        return Result.ok(planService.merchants(id));
    }

    @Operation(summary = "计划关联商户分页", description = "按商户名称与巡查状态分页返回该计划覆盖的商户")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/plan/{id}/merchant/page")
    public Result<Page<PlanMerchantVO>> merchantPage(
            @Parameter(description = "计划 ID", required = true) @PathVariable Long id,
            PlanMerchantQueryDTO query) {
        return Result.ok(planService.merchantPage(id, query));
    }

    @Operation(summary = "计划巡检结果", description = "分页返回该计划的巡检执行结果")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/plan/{id}/results")
    public Result<Page<PlanResultVO>> results(
            @Parameter(description = "计划 ID", required = true) @PathVariable Long id,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(planService.results(id, pageNum, pageSize));
    }
}
