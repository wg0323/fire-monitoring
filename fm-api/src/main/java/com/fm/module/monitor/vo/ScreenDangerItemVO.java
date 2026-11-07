package com.fm.module.monitor.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 大屏未处置隐患滚动项。
 */
@Data
@Schema(description = "大屏未处置隐患滚动项")
public class ScreenDangerItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "隐患类型")
    private String dangerType;
    @Schema(description = "隐患类型名称")
    private String dangerTypeName;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "来源类型")
    private Integer sourceType;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "处置条数")
    private LocalDate dealLimit;
}
