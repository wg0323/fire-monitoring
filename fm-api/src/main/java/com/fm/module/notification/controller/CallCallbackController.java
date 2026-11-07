package com.fm.module.notification.controller;

import com.fm.module.notification.service.CallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * 容联七陌外呼回调：仅按 actionId 更新通话记录，不写时间线。
 * 该接口由第三方服务器调用，无登录态，故不在 Sa-Token 拦截范围内；
 * 必须携带与 fm.callback.token 一致的令牌，否则拒绝全部回调。
 * 开发环境默认值见 application.yml，生产必须注入 FM_CALLBACK_TOKEN 覆盖。
 */
@Tag(name = "外呼回调", description = "第三方外呼平台回调，仅按 actionId 更新通话记录")
@Slf4j
@RestController
@RequestMapping("/api/callback/call")
public class CallCallbackController {

    private final CallService callService;
    private final String callbackToken;

    public CallCallbackController(CallService callService,
                                  @Value("${fm.callback.token:}") String callbackToken) {
        this.callService = callService;
        this.callbackToken = callbackToken;
    }

    @Operation(summary = "外呼回调", description = "第三方服务器调用；必须携带与 fm.callback.token 一致的令牌")
    @ApiResponse(responseCode = "200", description = "处理完成，body 中 code=0 表示成功")
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> callback(
            @Parameter(description = "通话记录动作 ID", required = false) @RequestParam(value = "ActionID", required = false) String actionId,
            @Parameter(description = "被叫号码", required = false) @RequestParam(value = "CalledNo", required = false) String calledNo,
            @Parameter(description = "通话状态", required = false) @RequestParam(value = "State", required = false) String state,
            @Parameter(description = "通话开始时间", required = false) @RequestParam(value = "Begin", required = false) String begin,
            @Parameter(description = "通话结束时间", required = false) @RequestParam(value = "End", required = false) String end,
            @Parameter(description = "录音文件名", required = false) @RequestParam(value = "MonitorFilename", required = false) String monitorFilename,
            @Parameter(description = "回调令牌（查询参数）", required = false) @RequestParam(value = "token", required = false) String queryToken,
            @Parameter(description = "回调令牌（请求头）", required = false) @RequestHeader(value = "X-Callback-Token", required = false) String headerToken) {
        Map<String, Object> result = new HashMap<>(2);
        if (!isAllowed(queryToken, headerToken)) {
            result.put("code", 401);
            result.put("message", "invalid callback token");
            return result;
        }
        callService.handleCallback(actionId, calledNo, state, begin, end, monitorFilename);
        result.put("code", 0);
        result.put("message", "success");
        return result;
    }

    private boolean isAllowed(String queryToken, String headerToken) {
        if (!StringUtils.hasText(callbackToken)) {
            log.warn("call callback rejected: fm.callback.token is not configured");
            return false;
        }
        return matches(queryToken) || matches(headerToken);
    }

    private boolean matches(String token) {
        return StringUtils.hasText(token) && MessageDigest.isEqual(
                callbackToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }
}
