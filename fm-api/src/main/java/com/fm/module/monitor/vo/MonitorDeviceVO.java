package com.fm.module.monitor.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电气/水压设备实时数据。
 */
@Data
@Schema(description = "电气/水压设备实时数据")
public class MonitorDeviceVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "设备主键ID")
    private Long deviceId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "设备编号")
    private String sn;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "位置")
    private String position;
    @Schema(description = "在线状态")
    private Integer onlineStatus;
    @Schema(description = "最后时间")
    private LocalDateTime lastTime;
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
    @Schema(description = "字段 powerA")
    private Integer powerA;
    @Schema(description = "字段 powerB")
    private Integer powerB;
    @Schema(description = "字段 powerC")
    private Integer powerC;
    @Schema(description = "字段 energyA")
    private Long energyA;
    @Schema(description = "字段 energyB")
    private Long energyB;
    @Schema(description = "字段 energyC")
    private Long energyC;
}
