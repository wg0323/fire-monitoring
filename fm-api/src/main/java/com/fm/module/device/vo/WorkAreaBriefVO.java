package com.fm.module.device.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商户详情中的工作区摘要。
 */
@Data
@Schema(description = "商户详情中的工作区摘要")
public class WorkAreaBriefVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "备注")
    private String remark;
}
