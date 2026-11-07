package com.fm.module.danger.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 隐患可指派外勤选项。
 */
@Data
@Schema(description = "隐患可指派外勤选项")
public class DangerAssigneeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "手机号")
    private String phone;
}
