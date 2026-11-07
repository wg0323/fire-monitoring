package com.fm.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.system.dto.LoginLogQueryDTO;
import com.fm.module.system.service.LoginLogService;
import com.fm.module.system.vo.LoginLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志接口。
 */
@Tag(name = "登录日志", description = "后台登录日志查询")
@RestController
@RequestMapping("/api/admin/log")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志", description = "按用户、结果与时间范围分页返回登录记录")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/login/page")
    public Result<Page<LoginLogVO>> page(LoginLogQueryDTO query) {
        return Result.ok(loginLogService.page(query));
    }
}
