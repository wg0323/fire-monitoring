package com.fm.module.alarm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 火警处置 KPI。
 */
@Data
@Schema(description = "火警处置 KPI")
public class AlarmKpiVO {

    /** 未处理火警 */
    @Schema(description = "未处理火警")
    private Long unhandledFire;
    /** 商户报火警待复核 */
    @Schema(description = "商户报火警待复核")
    private Long pendingReview;
    /** 核警中 */
    @Schema(description = "核警中")
    private Long verifying;
    /** 今日已处理火警 */
    @Schema(description = "今日已处理火警")
    private Long todayHandled;
    /** 今日平均响应（分钟，保留 1 位小数） */
    @Schema(description = "今日平均响应（分钟，保留 1 位小数）")
    private Double avgResponseMinutes;
}
