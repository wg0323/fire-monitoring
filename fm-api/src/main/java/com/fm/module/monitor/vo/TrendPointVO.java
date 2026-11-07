package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 趋势图单个时间桶。
 */
@Data
@Schema(description = "趋势图单个时间桶")
public class TrendPointVO {

    @Schema(description = "时间")
    private String time;
    @Schema(description = "数值")
    private BigDecimal value;
}
