package com.fm.module.workorder.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 后台工单接口：只读列表，派单生成，外勤在小程序接单/完成。
 */
@Tag(name = "工单管理", description = "后台工单查询与派单生成，外勤在小程序接单完成")
@RestController
@RequestMapping("/api/admin/workorder")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @Operation(summary = "分页查询工单", description = "按企业、状态与时间范围分页返回工单")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<WorkOrderVO>> page(WorkOrderQueryDTO query) {
        return Result.ok(workOrderService.pageVO(query));
    }

    @Operation(summary = "工单详情", description = "按工单 ID 返回详情，不存在时返回业务异常")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<WorkOrderVO> detail(@Parameter(description = "工单 ID", required = true) @PathVariable Long id) {
        return Result.ok(workOrderService.detailVO(id));
    }

    @Operation(summary = "接单", description = "外勤接单，需 workorder:accept 权限")
    @ApiResponse(responseCode = "200", description = "接单成功")
    @SaCheckPermission("workorder:accept")
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@Parameter(description = "工单 ID", required = true) @PathVariable Long id) {
        workOrderService.accept(id);
        return Result.ok();
    }

    @Operation(summary = "完成工单", description = "提交完成信息，需 workorder:finish 权限")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @SaCheckPermission("workorder:finish")
    @PutMapping("/finish")
    public Result<Void> finish(@Valid @RequestBody WorkOrderFinishDTO request) {
        workOrderService.finish(request);
        return Result.ok();
    }
}
