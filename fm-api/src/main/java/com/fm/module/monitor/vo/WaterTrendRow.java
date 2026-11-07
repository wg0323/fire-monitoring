package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 水系统历史按时间桶聚合结果。
 */
@Data
@Schema(description = "水系统历史按时间桶聚合结果")
public class WaterTrendRow {

    @Schema(description = "时间桶")
    private String bucket;
    @Schema(description = "测量类型")
    private Integer measureType;
    @Schema(description = "平均值数值")
    private BigDecimal avgValue;
    @Schema(description = "最大值数值")
    private BigDecimal maxValue;
    @Schema(description = "最小值数值")
    private BigDecimal minValue;
}
