package com.fm.module.miniapp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.miniapp.MpConstants;
import com.fm.module.workorder.dto.WorkOrderFinishDTO;
import com.fm.module.workorder.dto.WorkOrderQueryDTO;
import com.fm.module.workorder.service.WorkOrderService;
import com.fm.module.workorder.vo.WorkOrderVO;
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

/**
 * 小程序工单：外勤查看我的工单并接单/完成。
 */
@Tag(name = "小程序工单", description = "外勤查看我的工单并接单/完成")
@RestController
@RequestMapping("/api/mp/workorder")
@RequiredArgsConstructor
public class MpWorkOrderController {

    private final WorkOrderService workOrderService;

    @Operation(summary = "分页查询我的工单", description = "仅运营人员可处理工单")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<WorkOrderVO>> page(WorkOrderQueryDTO query) {
        requireTenantOperator();
        return Result.ok(workOrderService.mpPage(query));
    }

    @Operation(summary = "工单详情", description = "按工单 ID 返回详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<WorkOrderVO> detail(@Parameter(description = "工单 ID", required = true) @PathVariable Long id) {
        requireTenantOperator();
        return Result.ok(workOrderService.detailVO(id));
    }

    @Operation(summary = "接单", description = "外勤接单")
    @ApiResponse(responseCode = "200", description = "接单成功")
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@Parameter(description = "工单 ID", required = true) @PathVariable Long id) {
        requireTenantOperator();
        workOrderService.accept(id);
        return Result.ok();
    }

    @Operation(summary = "完成工单", description = "提交完成信息")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @PostMapping("/{id}/finish")
    public Result<Void> finish(@Parameter(description = "工单 ID", required = true) @PathVariable Long id,
                               @Valid @RequestBody WorkOrderFinishDTO request) {
        requireTenantOperator();
        request.setId(id);
        workOrderService.finish(request);
        return Result.ok();
    }

    private void requireTenantOperator() {
        if (!MpConstants.USER_TYPE_TENANT.equals(TenantContext.getUserType())) {
            throw new BizException(403, "仅运营人员可处理工单");
        }
    }
}
