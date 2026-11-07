package com.fm.module.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 下行指令下发请求。
 */
@Schema(description = "下行指令下发请求")
@Data
public class CommandCreateDTO {

    @Schema(description = "设备 SN", example = "SN0001")
    @NotBlank
    private String sn;

    /** TIME_SYNC / PARAM_SET / PARAM_QUERY / SILENCE */
    @Schema(description = "指令类型：TIME_SYNC / PARAM_SET / PARAM_QUERY / SILENCE")
    @NotBlank
    private String commandType;

    /** 指令参数 JSON 字符串，可为空 */
    @Schema(description = "指令参数 JSON 字符串，可为空")
    private String params;

    @Schema(description = "超时秒数")
    private Integer timeoutSeconds;
}
