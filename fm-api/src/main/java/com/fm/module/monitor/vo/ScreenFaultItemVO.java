package com.fm.module.monitor.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大屏故障滚动项。
 */
@Data
@Schema(description = "大屏故障滚动项")
public class ScreenFaultItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "设备设备编号")
    private String deviceSn;
    @Schema(description = "设备名称")
    private String deviceName;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "位置")
    private String position;
    @Schema(description = "报警时间")
    private LocalDateTime alarmTime;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "恢复时间")
    private LocalDateTime recoverTime;
}
