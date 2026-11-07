package com.fm.module.monitor.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大屏实时报警滚动项。
 */
@Data
@Schema(description = "大屏实时报警滚动项")
public class ScreenAlarmItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "报警时间")
    private LocalDateTime alarmTime;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "位置")
    private String position;
    @Schema(description = "报警级别")
    private Integer alarmLevel;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "结果")
    private Integer result;
    @Schema(description = "经度")
    private Double lng;
    @Schema(description = "纬度")
    private Double lat;
}
