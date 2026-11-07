package com.fm.module.alarm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.api.Result;
import com.fm.module.alarm.converter.CommandConverter;
import com.fm.module.alarm.dto.CommandCreateDTO;
import com.fm.module.alarm.service.CommandService;
import com.fm.module.alarm.vo.CommandTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 下行指令接口：下发后返回任务 id，状态经回执异步更新。
 */
@Tag(name = "下行指令", description = "向设备下发指令，状态经回执异步更新")
@RestController
@RequestMapping("/api/admin/command")
@RequiredArgsConstructor
public class CommandController {

    private final CommandService commandService;

    @Operation(summary = "下发指令", description = "下发后返回任务 ID，执行状态由回执更新")
    @ApiResponse(responseCode = "200", description = "下发成功")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CommandCreateDTO request) {
        return Result.ok(commandService.create(request));
    }

    @Operation(summary = "分页查询指令任务", description = "按设备 SN 与执行状态分页返回指令任务")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<CommandTaskVO>> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "设备 SN，可为空") @RequestParam(required = false) String sn,
            @Parameter(description = "执行状态，可为空") @RequestParam(required = false) String status) {
        return Result.ok(CommandConverter.toVOPage(commandService.page(pageNum, pageSize, sn, status)));
    }
}
