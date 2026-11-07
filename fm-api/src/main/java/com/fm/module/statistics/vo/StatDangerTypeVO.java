package com.fm.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 隐患分析按类型明细：完成率与逾期率由后端统一换算。
 */
@Data
@Schema(description = "隐患分析按类型明细：完成率与逾期率由后端统一换算")
public class StatDangerTypeVO {

    @Schema(description = "隐患类型")
    private String dangerType;
    @Schema(description = "隐患类型名称")
    private String dangerTypeName;
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
}
