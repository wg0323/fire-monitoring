package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 电气火灾历史按时间桶聚合结果（A 相）。
 */
@Data
@Schema(description = "电气火灾历史按时间桶聚合结果（A 相）")
public class ElectricTrendRow {

    @Schema(description = "时间桶")
    private String bucket;
    @Schema(description = "电压平均值")
    private BigDecimal voltageAvg;
    @Schema(description = "电压最大值")
    private BigDecimal voltageMax;
    @Schema(description = "电压最小值")
    private BigDecimal voltageMin;
    @Schema(description = "当前平均值")
    private BigDecimal currentAvg;
    @Schema(description = "当前最大值")
    private BigDecimal currentMax;
    @Schema(description = "当前最小值")
    private BigDecimal currentMin;
    @Schema(description = "剩余电流平均值")
    private BigDecimal residualAvg;
    @Schema(description = "剩余电流最大值")
    private BigDecimal residualMax;
    @Schema(description = "剩余电流最小值")
    private BigDecimal residualMin;
    @Schema(description = "温度平均值")
    private BigDecimal temperatureAvg;
    @Schema(description = "温度最大值")
    private BigDecimal temperatureMax;
    @Schema(description = "温度最小值")
    private BigDecimal temperatureMin;
}
