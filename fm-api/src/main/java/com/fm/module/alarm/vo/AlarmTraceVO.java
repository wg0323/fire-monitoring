package com.fm.module.alarm.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报警时间线展示对象。
 */
@Data
@Schema(description = "报警时间线展示对象")
public class AlarmTraceVO {

    /** 主键ID */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "轨迹类型")
    private Integer traceType;
    @Schema(description = "结果")
    private Integer result;
    @Schema(description = "内容")
    private String content;
    @Schema(description = "详情")
    private String detail;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
