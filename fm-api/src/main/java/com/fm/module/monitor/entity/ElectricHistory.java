package com.fm.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电气火灾历史数据。
 */
@Data
@TableName("b_electric_history")
public class ElectricHistory {

    @TableId(value = "f_id", type = IdType.AUTO)
    private Long id;
    @TableField("f_sn")
    private String sn;
    @TableField("f_phase_type")
    private Integer phaseType;
    @TableField("f_voltage_a")
    private Integer voltageA;
    @TableField("f_voltage_b")
    private Integer voltageB;
    @TableField("f_voltage_c")
    private Integer voltageC;
    @TableField("f_current_a")
    private Integer currentA;
    @TableField("f_current_b")
    private Integer currentB;
    @TableField("f_current_c")
    private Integer currentC;
    @TableField("f_current_n")
    private Integer currentN;
    @TableField("f_power_a")
    private Integer powerA;
    @TableField("f_power_b")
    private Integer powerB;
    @TableField("f_power_c")
    private Integer powerC;
    @TableField("f_live_voltage")
    private Integer liveVoltage;
    @TableField("f_live_current")
    private Integer liveCurrent;
    @TableField("f_residual_current")
    private Integer residualCurrent;
    @TableField("f_temperature")
    private Integer temperature;
    @TableField("f_measure_time")
    private LocalDateTime measureTime;
    @TableField("f_create_time")
    private LocalDateTime createTime;
}
