package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 档案趋势序列：水/电设备详情与电子档案共用。
 */
@Data
@Schema(description = "档案趋势序列：水/电设备详情与电子档案共用")
public class MonitorTrendVO {

    @Schema(description = "字段 metric")
    private String metric;
    @Schema(description = "标签")
    private String label;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "最大值")
    private BigDecimal max;
    @Schema(description = "最小值")
    private BigDecimal min;
    @Schema(description = "平均值")
    private BigDecimal avg;
    @Schema(description = "是否数据")
    private boolean hasData;
    @Schema(description = "时间桶数据点列表")
    private List<TrendPointVO> points = new ArrayList<>();
}
