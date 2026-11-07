package com.fm.module.alarm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.alarm.converter.AlarmConverter;
import com.fm.module.alarm.dto.AlarmCallDTO;
import com.fm.module.alarm.dto.AlarmDispatchDTO;
import com.fm.module.alarm.dto.AlarmQueryDTO;
import com.fm.module.alarm.dto.AlarmVerifyDTO;
import com.fm.module.alarm.service.AlarmService;
import com.fm.module.alarm.vo.AlarmDispatchOptionVO;
import com.fm.module.alarm.vo.AlarmKpiVO;
import com.fm.module.alarm.vo.AlarmMerchantVO;
import com.fm.module.alarm.vo.AlarmTraceVO;
import com.fm.module.alarm.vo.AlarmVO;
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

/**
 * 报警管理接口：火警处置、报警列表、跟踪与故障转隐患。
 */
@Tag(name = "报警管理", description = "火警处置、报警列表、跟踪与故障转隐患")
@RestController
@RequestMapping("/api/admin/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "分页查询报警", description = "按企业、级别、状态与时间范围分页返回报警")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<AlarmVO>> page(AlarmQueryDTO query) {
        return Result.ok(alarmService.pageVO(query));
    }

    @Operation(summary = "报警 KPI", description = "返回报警处置台顶部指标")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/kpi")
    public Result<AlarmKpiVO> kpi() {
        return Result.ok(alarmService.kpi());
    }

    @Operation(summary = "报警企业列表", description = "按关键字搜索企业，可只返回有未核警报警的企业")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/merchant-list")
    public Result<List<AlarmMerchantVO>> merchantList(
            @Parameter(description = "企业名称关键字，可为空") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "仅返回有未核警报警的企业，火警处置台传 true")
            @RequestParam(value = "unverifiedOnly", required = false) Boolean unverifiedOnly) {
        return Result.ok(alarmService.merchantList(keyword, unverifiedOnly));
    }

    @Operation(summary = "派单可选人员", description = "返回当前可派单的处理人列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/dispatch-options")
    public Result<List<AlarmDispatchOptionVO>> dispatchOptions() {
        return Result.ok(alarmService.dispatchOptions());
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
    public Result<List<AlarmTraceVO>> timeline(@Parameter(description = "报警 ID", required = true) @PathVariable Long id) {
        return Result.ok(AlarmConverter.toTraceVOList(alarmService.timeline(id)));
    }

    @Operation(summary = "核警", description = "核实报警真伪，需 alarm:verify 权限")
    @ApiResponse(responseCode = "200", description = "核警成功")
    @SaCheckPermission("alarm:verify")
    @PostMapping("/verify")
    public Result<Void> verify(@Valid @RequestBody AlarmVerifyDTO request) {
        alarmService.verify(request);
        return Result.ok();
    }

    @Operation(summary = "派单", description = "把报警派给处理人，需 alarm:dispatch 权限")
    @ApiResponse(responseCode = "200", description = "派单成功")
    @SaCheckPermission("alarm:dispatch")
    @PostMapping("/dispatch")
    public Result<Void> dispatch(@Valid @RequestBody AlarmDispatchDTO request) {
        alarmService.dispatch(request);
        return Result.ok();
    }

    @Operation(summary = "登记通话结果", description = "登记电话告警/电话核警的接通结果并写入报警追踪表，需 alarm:call 权限")
    @ApiResponse(responseCode = "200", description = "登记成功")
    @SaCheckPermission("alarm:call")
    @PostMapping("/{id}/call")
    public Result<Void> call(@Parameter(description = "报警 ID", required = true) @PathVariable Long id,
                             @Valid @RequestBody AlarmCallDTO request) {
        request.setId(id);
        alarmService.call(request);
        return Result.ok();
    }

    @Operation(summary = "故障转隐患", description = "把报警转为隐患并返回隐患编号，需 alarm:convert-danger 权限")
    @ApiResponse(responseCode = "200", description = "转换成功")
    @SaCheckPermission("alarm:convert-danger")
    @PostMapping("/{id}/convert-danger")
    public Result<String> convertDanger(
            @Parameter(description = "报警 ID", required = true) @PathVariable Long id) {
        return Result.ok(String.valueOf(alarmService.convertDanger(id)));
    }
}
