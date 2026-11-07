package com.fm.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计趋势点：按天补零后的计数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统计趋势点：按天补零后的计数")
public class StatTrendPointVO {

    @Schema(description = "时间")
    private String time;
    @Schema(description = "数值")
    private Long value;
}
