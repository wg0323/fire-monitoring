package com.fm.module.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 商户处置请求：火警 / 误报。故障由设备上报确定，后续走隐患流程。
 */
@Schema(description = "商户处置报警请求")
@Data
public class AlarmMerchantHandleDTO {

    /** 由小程序路径参数写入 */
    @Schema(description = "报警 ID，由路径参数写入")
    private Long id;

    /** 0 误报，1 火警；故障由设备上报确定，不作为处置结论 */
    @Schema(description = "处置结论：0 误报，1 火警", example = "1")
    @NotNull(message = "请选择处置结论")
    @Min(value = 0, message = "处置结论不合法")
    @Max(value = 1, message = "处置结论不合法")
    private Integer result;

    @Schema(description = "处置说明")
    private String remark;
    @Schema(description = "现场照片 1")
    private String pic1;
    @Schema(description = "现场照片 2")
    private String pic2;
    @Schema(description = "现场照片 3")
    private String pic3;
}
