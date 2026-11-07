package com.fm.module.miniapp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.miniapp.MpConstants;
import com.fm.module.notification.dto.SubscribeDTO;
import com.fm.module.notification.service.MpNotificationService;
import com.fm.module.notification.vo.MpNotificationVO;
import com.fm.module.notification.vo.SubscribeConfigVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 小程序站内消息与订阅配置。
 */
@Tag(name = "小程序消息", description = "站内消息查询与微信订阅配置")
@RestController
@RequestMapping("/api/mp/notification")
@RequiredArgsConstructor
public class MpNotificationController {

    private final MpNotificationService notificationService;

    @Operation(summary = "分页查询我的消息", description = "按类型分页返回当前账号的站内消息")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<MpNotificationVO>> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "消息类型，可为空") @RequestParam(required = false) Integer type) {
        requireMpUser();
        return Result.ok(notificationService.myPage(pageNum, pageSize, type));
    }

    @Operation(summary = "未读消息数", description = "返回当前账号的未读消息条数")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        requireMpUser();
        return Result.ok(notificationService.unreadCount());
    }

    @Operation(summary = "标记已读", description = "按消息 ID 标记已读")
    @ApiResponse(responseCode = "200", description = "标记成功")
    @PutMapping("/{id}/read")
    public Result<Void> read(@Parameter(description = "消息 ID", required = true) @PathVariable Long id) {
        requireMpUser();
        notificationService.markRead(id);
        return Result.ok();
    }

    @Operation(summary = "订阅配置", description = "返回微信订阅消息模板配置")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/subscribe-config")
    public Result<SubscribeConfigVO> subscribeConfig() {
        requireMpUser();
        return Result.ok(notificationService.subscribeConfig());
    }

    @Operation(summary = "记录订阅", description = "记录用户已授权的订阅消息模板")
    @ApiResponse(responseCode = "200", description = "记录成功")
    @PostMapping("/subscribe")
    public Result<Void> subscribe(@Valid @RequestBody SubscribeDTO request) {
        requireMpUser();
        notificationService.recordSubscribe(request.getTemplateIds());
        return Result.ok();
    }

    private void requireMpUser() {
        if (!MpConstants.isMiniappUserType(TenantContext.getUserType())) {
            throw new BizException(403, "仅小程序账号可查看消息");
        }
    }
}
