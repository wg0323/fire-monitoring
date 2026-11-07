package com.fm.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 综合分析：区间总数、报警趋势与三类排行。
 */
@Data
@Schema(description = "综合分析：区间总数、报警趋势与三类排行")
public class StatComprehensiveVO {

    @Schema(description = "报警总数")
    private Long alarmTotal;
    @Schema(description = "故障总数")
    private Long faultTotal;
    @Schema(description = "隐患总数")
    private Long dangerTotal;
    @Schema(description = "火警总数")
    private Long fireTotal;
    @Schema(description = "外呼总数")
    private Long callTotal;
    @Schema(description = "报警趋势")
    private List<StatTrendPointVO> alarmTrend;
    @Schema(description = "隐患类型排行")
    private List<StatRankingVO> dangerTypeRanking;
    @Schema(description = "故障商户排行")
    private List<StatRankingVO> faultMerchantRanking;
    @Schema(description = "外呼商户排行")
    private List<StatRankingVO> callMerchantRanking;
}
