package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 大屏趋势点：时间桶与数量。
 */
@Data
@Schema(description = "大屏趋势点：时间桶与数量")
public class ScreenTrendPointVO {

    @Schema(description = "时间")
    private String time;
    @Schema(description = "数值")
    private Long value;

    public ScreenTrendPointVO() {
    }

    public ScreenTrendPointVO(String time, Long value) {
        this.time = time;
        this.value = value;
    }
}
