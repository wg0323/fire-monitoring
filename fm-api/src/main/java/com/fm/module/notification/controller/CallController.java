package com.fm.module.notification.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fm.common.api.Result;
import com.fm.module.notification.dto.CallRequestDTO;
import com.fm.module.notification.service.CallService;
import com.fm.module.notification.vo.CallResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 外呼发起接口：结果同步返回，通话明细由七陌回调更新。
 */
@Tag(name = "外呼", description = "发起外呼，通话明细由第三方回调更新")
@RestController
@RequestMapping("/api/admin/call")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    @Operation(summary = "发起外呼", description = "按被叫号码发起外呼，同步返回呼叫结果，需 alarm:call 权限")
    @ApiResponse(responseCode = "200", description = "呼叫已发起")
    @SaCheckPermission("alarm:call")
    @PostMapping
    public Result<CallResultVO> dialOut(@Valid @RequestBody CallRequestDTO request) {
        return Result.ok(callService.dialOut(request));
    }
}
