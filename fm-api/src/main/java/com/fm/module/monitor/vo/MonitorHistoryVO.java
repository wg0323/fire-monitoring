package com.fm.module.monitor.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电气/水压历史点。
 */
@Data
@Schema(description = "电气/水压历史点")
public class MonitorHistoryVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "测量时间")
    private LocalDateTime measureTime;
    @Schema(description = "测量数值")
    private Integer measureValue;
    @Schema(description = "测量类型")
    private Integer measureType;
    @Schema(description = "phase类型")
    private Integer phaseType;
    @Schema(description = "live电压")
    private Integer liveVoltage;
    @Schema(description = "live当前")
    private Integer liveCurrent;
    @Schema(description = "剩余电流当前")
    private Integer residualCurrent;
    @Schema(description = "温度")
    private Integer temperature;
    @Schema(description = "电压a")
    private Integer voltageA;
    @Schema(description = "电压b")
    private Integer voltageB;
    @Schema(description = "电压c")
    private Integer voltageC;
    @Schema(description = "当前a")
    private Integer currentA;
    @Schema(description = "当前b")
    private Integer currentB;
    @Schema(description = "当前c")
    private Integer currentC;
}
