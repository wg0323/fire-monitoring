package com.fm.module.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 核警 / 商户火警复核请求：结论 + 说明 + 现场照片。
 */
@Schema(description = "核警/商户火警复核请求")
@Data
public class AlarmVerifyDTO {

    /** 由后台请求体或小程序路径参数写入 */
    @Schema(description = "报警 ID，由路径参数写入")
    private Long id;

    /** 0 误报，1 真实火警；故障由设备上报确定，不作为核警结论 */
    @Schema(description = "核警结论：0 误报，1 真实火警", example = "1")
    @NotNull(message = "核警结论不能为空")
    @Min(value = 0, message = "核警结论不合法")
    @Max(value = 1, message = "核警结论不合法")
    private Integer result;

    @Schema(description = "核警说明")
    private String remark;
    @Schema(description = "现场照片 1")
    private String pic1;
    @Schema(description = "现场照片 2")
    private String pic2;
    @Schema(description = "现场照片 3")
    private String pic3;
}
