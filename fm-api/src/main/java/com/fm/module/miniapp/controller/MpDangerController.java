package com.fm.module.miniapp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.danger.dto.DangerAssignDTO;
import com.fm.module.danger.dto.DangerAuditDTO;
import com.fm.module.danger.dto.DangerDealDTO;
import com.fm.module.danger.dto.DangerQueryDTO;
import com.fm.module.danger.dto.DangerSaveDTO;
import com.fm.module.danger.service.DangerService;
import com.fm.module.danger.vo.DangerAssigneeVO;
import com.fm.module.danger.vo.DangerTimelineVO;
import com.fm.module.danger.vo.DangerVO;
import com.fm.module.miniapp.MpConstants;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

/**
 * 小程序隐患接口：按角色收窄范围，商户可随手拍上报本店隐患。
 */
@Tag(name = "小程序隐患", description = "按角色收窄范围，商户可随手拍上报本店隐患")
@RestController
@RequestMapping("/api/mp/danger")
@RequiredArgsConstructor
public class MpDangerController {

    private final DangerService dangerService;

    @Operation(summary = "分页查询隐患", description = "按当前登录账号的角色收窄可见范围")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<DangerVO>> page(DangerQueryDTO query) {
        requireMpUser();
        return Result.ok(dangerService.page(query));
    }

    @Operation(summary = "隐患详情", description = "按隐患 ID 返回详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<DangerVO> detail(@Parameter(description = "隐患 ID", required = true) @PathVariable Long id) {
        requireMpUser();
        return Result.ok(dangerService.detail(id));
    }

    @Operation(summary = "隐患时间线", description = "按隐患 ID 返回处置记录")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/timeline")
    public Result<List<DangerTimelineVO>> timeline(
            @Parameter(description = "隐患 ID", required = true) @PathVariable Long id) {
        requireMpUser();
        return Result.ok(dangerService.timeline(id));
    }

    @Operation(summary = "上报隐患", description = "商户只能上报本商户的隐患")
    @ApiResponse(responseCode = "200", description = "上报成功")
    @PostMapping
    public Result<Long> report(@Valid @RequestBody DangerSaveDTO request) {
        requireMpUser();
        if (MpConstants.USER_TYPE_MERCHANT.equals(TenantContext.getUserType())
                && !Objects.equals(request.getMerchantId(), TenantContext.getMerchantId())) {
            throw new BizException(403, "只能上报本商户的隐患");
        }
        return Result.ok(dangerService.report(request));
    }

    @Operation(summary = "整改隐患", description = "提交整改结果")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @PostMapping("/{id}/deal")
    public Result<Void> deal(@Parameter(description = "隐患 ID", required = true) @PathVariable Long id,
                             @Valid @RequestBody DangerDealDTO request) {
        requireMpUser();
        dangerService.deal(id, request);
        return Result.ok();
    }

    @Operation(summary = "指派隐患", description = "仅租户管理员可执行")
    @ApiResponse(responseCode = "200", description = "指派成功")
    @PostMapping("/{id}/assign")
    public Result<Void> assign(@Parameter(description = "隐患 ID", required = true) @PathVariable Long id,
                               @Valid @RequestBody DangerAssignDTO request) {
        requireTenantAdmin();
        dangerService.assign(id, request);
        return Result.ok();
    }

    @Operation(summary = "审核隐患", description = "仅租户管理员可执行")
    @ApiResponse(responseCode = "200", description = "审核成功")
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@Parameter(description = "隐患 ID", required = true) @PathVariable Long id,
                              @Valid @RequestBody DangerAuditDTO request) {
        requireTenantAdmin();
        dangerService.audit(id, request);
        return Result.ok();
    }

    @Operation(summary = "可选处理人", description = "按隐患 ID 返回可指派人员，仅租户管理员可执行")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/assignee-options")
    public Result<List<DangerAssigneeVO>> assigneeOptions(
            @Parameter(description = "隐患 ID", required = true) @RequestParam Long dangerId) {
        requireTenantAdmin();
        return Result.ok(dangerService.assigneeOptions(dangerId));
    }

    private void requireMpUser() {
        if (!MpConstants.isMiniappUserType(TenantContext.getUserType())) {
            throw new BizException(403, "当前账号不能访问小程序隐患");
        }
    }

    private void requireTenantAdmin() {
        if (!MpConstants.isTenantAdmin()) {
            throw new BizException(403, "仅租户管理员可执行隐患指派或审核");
        }
    }
}
