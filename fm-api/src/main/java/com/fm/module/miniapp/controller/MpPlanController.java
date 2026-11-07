package com.fm.module.miniapp.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.inspection.dto.PlanMerchantQueryDTO;
import com.fm.module.inspection.dto.PlanQueryDTO;
import com.fm.module.inspection.dto.PlanResultSaveDTO;
import com.fm.module.inspection.service.PlanService;
import com.fm.module.inspection.vo.PlanMerchantVO;
import com.fm.module.inspection.vo.PlanVO;
import com.fm.module.miniapp.MpConstants;
import com.fm.module.system.entity.SysRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 小程序巡查：外勤按所属工作区查看计划并逐商户提交结果。
 */
@Tag(name = "小程序巡查", description = "外勤按所属工作区查看计划并逐商户提交结果")
@RestController
@RequestMapping("/api/mp/inspection")
@RequiredArgsConstructor
public class MpPlanController {

    private final PlanService planService;

    @Operation(summary = "分页查询巡查计划", description = "仅外勤与租户管理员可执行")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/plan/page")
    public Result<Page<PlanVO>> page(PlanQueryDTO query) {
        requireInspectionUser();
        return Result.ok(planService.page(query));
    }

    @Operation(summary = "巡查计划详情", description = "按计划 ID 返回详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/plan/{id}")
    public Result<PlanVO> detail(@Parameter(description = "计划 ID", required = true) @PathVariable Long id) {
        requireInspectionUser();
        return Result.ok(planService.detail(id));
    }

    @Operation(summary = "计划关联商户", description = "返回该计划覆盖的商户列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/plan/{id}/merchants")
    public Result<List<PlanMerchantVO>> merchants(
            @Parameter(description = "计划 ID", required = true) @PathVariable Long id) {
        requireInspectionUser();
        return Result.ok(planService.merchants(id));
    }

    @Operation(summary = "计划关联商户分页", description = "按商户名称分页返回该计划覆盖的商户")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/plan/{id}/merchant/page")
    public Result<Page<PlanMerchantVO>> merchantPage(
            @Parameter(description = "计划 ID", required = true) @PathVariable Long id,
            PlanMerchantQueryDTO query) {
        requireInspectionUser();
        return Result.ok(planService.merchantPage(id, query));
    }

    @Operation(summary = "提交巡查结果", description = "按商户提交巡查结果，执行人取当前登录账号")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @PostMapping("/result")
    public Result<Void> submitResult(@Valid @RequestBody PlanResultSaveDTO request) {
        requireInspectionUser();
        planService.saveResult(request, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    private void requireInspectionUser() {
        if (!MpConstants.USER_TYPE_TENANT.equals(TenantContext.getUserType())) {
            throw new BizException(403, "仅外勤与租户管理员可执行巡查");
        }
        if (MpConstants.isTenantAdmin()) {
            return;
        }
        if (!SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            throw new BizException(403, "仅外勤与租户管理员可执行巡查");
        }
    }
}
