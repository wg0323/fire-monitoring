package com.fm.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 隐患分析：区间汇总指标与按类型明细。
 */
@Data
@Schema(description = "隐患分析：区间汇总指标与按类型明细")
public class StatDangerVO {

    @Schema(description = "总数")
    private Long total;
    @Schema(description = "已完成")
    private Long done;
    @Schema(description = "未完成")
    private Long undone;
    @Schema(description = "逾期")
    private Long overdue;
    @Schema(description = "已完成比率")
    private Double doneRate;
    @Schema(description = "逾期比率")
    private Double overdueRate;
    @Schema(description = "行数据")
    private List<StatDangerTypeVO> rows;
}
