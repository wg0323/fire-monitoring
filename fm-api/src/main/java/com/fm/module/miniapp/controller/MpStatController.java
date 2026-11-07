package com.fm.module.miniapp.controller;

import com.fm.common.api.Result;
import com.fm.module.statistics.service.StatService;
import com.fm.module.statistics.vo.OverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序概览接口：统计范围随登录账号自动收窄。
 */
@Tag(name = "小程序概览", description = "统计范围随登录账号自动收窄")
@RestController
@RequestMapping("/api/mp/stat")
@RequiredArgsConstructor
public class MpStatController {

    private final StatService statService;

    @Operation(summary = "概览统计", description = "返回当前账号可见范围内的概览 KPI")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/overview")
    public Result<OverviewVO> overview() {
        return Result.ok(statService.overview());
    }
}
