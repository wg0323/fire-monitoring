package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 巡查隐患大屏：当月指标、趋势、滚动、巡检、排行与地图点。
 */
@Data
@Schema(description = "巡查隐患大屏：当月指标、趋势、滚动、巡检、排行与地图点")
public class ScreenDangerVO {

    @Schema(description = "月总数")
    private Long monthTotal;
    @Schema(description = "月已完成")
    private Long monthDone;
    @Schema(description = "月未完成")
    private Long monthUnfinished;
    @Schema(description = "月逾期")
    private Long monthOverdue;
    @Schema(description = "趋势")
    private List<ScreenTrendPointVO> trend;
    @Schema(description = "隐患")
    private List<ScreenDangerItemVO> dangers;
    @Schema(description = "计划数量")
    private Long planCount;
    @Schema(description = "计划已完成")
    private Long planDone;
    @Schema(description = "计划隐患")
    private Long planDanger;
    @Schema(description = "类型排行")
    private List<ScreenTypeCountVO> typeRanking;
    @Schema(description = "商户点位")
    private List<MerchantStatusPointVO> merchantPoints;
}
