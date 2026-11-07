package com.fm.module.miniapp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.converter.AlarmConverter;
import com.fm.module.alarm.dto.AlarmDispatchDTO;
import com.fm.module.alarm.dto.AlarmMerchantHandleDTO;
import com.fm.module.alarm.dto.AlarmQueryDTO;
import com.fm.module.alarm.dto.AlarmVerifyDTO;
import com.fm.module.alarm.service.AlarmService;
import com.fm.module.alarm.vo.AlarmTraceVO;
import com.fm.module.alarm.vo.AlarmVO;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 小程序报警接口：商户处置本店报警，租户管理员复核与派单，外勤只读。
 */
@Tag(name = "小程序报警", description = "商户处置本店报警，租户管理员复核与派单，外勤只读")
@RestController
@RequestMapping("/api/mp/alarm")
@RequiredArgsConstructor
public class MpAlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "分页查询报警", description = "按当前登录账号的角色收窄可见范围")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<AlarmVO>> page(AlarmQueryDTO query) {
        return Result.ok(alarmService.pageVO(query));
    }

    @Operation(summary = "报警详情", description = "按报警 ID 返回详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public Result<AlarmVO> detail(@Parameter(description = "报警 ID", required = true) @PathVariable Long id) {
        return Result.ok(alarmService.detail(id));
    }

    @Operation(summary = "报警时间线", description = "按报警 ID 返回处置轨迹")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/timeline")
    public Result<List<AlarmTraceVO>> timeline(
            @Parameter(description = "报警 ID", required = true) @PathVariable Long id) {
        return Result.ok(AlarmConverter.toTraceVOList(alarmService.timeline(id)));
    }

    @Operation(summary = "商户处置报警", description = "商户提交本店报警的处置结果")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @PostMapping("/{id}/merchant-handle")
    public Result<Void> merchantHandle(@Parameter(description = "报警 ID", required = true) @PathVariable Long id,
                                       @Valid @RequestBody AlarmMerchantHandleDTO request) {
        request.setId(id);
        alarmService.merchantHandle(request);
        return Result.ok();
    }

    @Operation(summary = "复核报警", description = "仅租户管理员可执行")
    @ApiResponse(responseCode = "200", description = "复核成功")
    @PostMapping("/{id}/verify")
    public Result<Void> verify(@Parameter(description = "报警 ID", required = true) @PathVariable Long id,
                               @Valid @RequestBody AlarmVerifyDTO request) {
        requireTenantAdmin();
        request.setId(id);
        alarmService.verify(request);
        return Result.ok();
    }

    @Operation(summary = "派单", description = "仅租户管理员可执行")
    @ApiResponse(responseCode = "200", description = "派单成功")
    @PostMapping("/{id}/dispatch")
    public Result<Void> dispatch(@Parameter(description = "报警 ID", required = true) @PathVariable Long id,
                                 @Valid @RequestBody AlarmDispatchDTO request) {
        requireTenantAdmin();
        request.setId(id);
        alarmService.dispatch(request);
        return Result.ok();
    }

    private void requireTenantAdmin() {
        if (!MpConstants.isTenantAdmin()) {
            throw new BizException(403, "仅租户管理员可执行复核或派单");
        }
    }
}
