package com.fm.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 运营概览：报警与设备的核心计数、分布与近 7 天趋势。
 */
@Data
@Schema(description = "运营概览：报警与设备的核心计数、分布与近 7 天趋势")
public class OverviewVO {

    @Schema(description = "报警总数")
    private long alarmTotal;
    @Schema(description = "报警今日")
    private long alarmToday;
    @Schema(description = "报警待处理")
    private long alarmPending;
    @Schema(description = "设备总数")
    private long deviceTotal;
    @Schema(description = "设备在线")
    private long deviceOnline;
    @Schema(description = "设备离线")
    private long deviceOffline;
    @Schema(description = "报警类型")
    private List<TypeCount> alarmByType;
    @Schema(description = "设备类型")
    private List<TypeCount> deviceByType;
    @Schema(description = "报警趋势")
    private List<TrendPoint> alarmTrend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "类型数量视图对象")
    public static class TypeCount {
        @Schema(description = "名称")
        private String name;
        @Schema(description = "数值")
        private long value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "趋势点位视图对象")
    public static class TrendPoint {
        @Schema(description = "日期")
        private String date;
        @Schema(description = "数值")
        private long value;
    }
}
