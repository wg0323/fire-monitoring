package com.fm.module.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.notification.dto.NotificationQueryDTO;
import com.fm.module.notification.service.MpNotificationService;
import com.fm.module.notification.vo.MpNotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端通知记录查询。
 */
@Tag(name = "通知记录", description = "管理端查询小程序通知发送记录")
@RestController
@RequestMapping("/api/admin/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final MpNotificationService notificationService;

    @Operation(summary = "分页查询通知记录", description = "按当前登录账号的可见范围分页返回通知记录")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<MpNotificationVO>> page(NotificationQueryDTO query) {
        return Result.ok(notificationService.adminPage(query));
    }
}
