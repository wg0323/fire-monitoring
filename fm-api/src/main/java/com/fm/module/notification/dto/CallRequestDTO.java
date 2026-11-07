package com.fm.module.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 外呼请求：type 1 电话告警，2 电话核警。
 */
@Schema(description = "外呼请求")
@Data
public class CallRequestDTO {

    @NotNull
    @Schema(description = "报警 ID")
    private Long alarmId;

    @NotBlank
    @Schema(description = "手机号")
    private String phone;

    @NotNull
    @Schema(description = "类型")
    private Integer type;
}
