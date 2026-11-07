package com.fm.module.device.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备详情页的最近报警条目。
 */
@Data
@Schema(description = "设备详情页的最近报警条目")
public class DeviceAlarmBriefVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "报警时间")
    private LocalDateTime alarmTime;
    @Schema(description = "报警类型")
    private String alarmType;
    @Schema(description = "报警类型名称")
    private String alarmTypeName;
    @Schema(description = "报警级别")
    private Integer alarmLevel;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "结果")
    private Integer result;
    @Schema(description = "位置")
    private String position;
}
