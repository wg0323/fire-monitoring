package com.fm.module.miniapp.controller;

import com.fm.common.api.Result;
import com.fm.module.miniapp.service.MpHomeService;
import com.fm.module.miniapp.vo.MpHomeSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序首页接口。
 */
@Tag(name = "小程序首页", description = "小程序首页 KPI 与最新列表")
@RestController
@RequestMapping("/api/mp/home")
@RequiredArgsConstructor
public class MpHomeController {

    private final MpHomeService mpHomeService;

    @Operation(summary = "首页汇总", description = "按登录角色返回首页 KPI 与最新列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/summary")
    public Result<MpHomeSummaryVO> summary() {
        return Result.ok(mpHomeService.summary());
    }
}
