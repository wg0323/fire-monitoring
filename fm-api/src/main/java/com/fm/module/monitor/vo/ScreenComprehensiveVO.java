package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 综合监控大屏：KPI、设备状态与商户地图点。
 */
@Data
@Schema(description = "综合监控大屏：KPI、设备状态与商户地图点")
public class ScreenComprehensiveVO {

    @Schema(description = "火警总数")
    private Long fireTotal;
    @Schema(description = "火警handled")
    private Long fireHandled;
    @Schema(description = "火警true")
    private Long fireTrue;
    @Schema(description = "隐患unhandled")
    private Long dangerUnhandled;
    @Schema(description = "隐患今日")
    private Long dangerToday;
    @Schema(description = "隐患逾期")
    private Long dangerOverdue;
    @Schema(description = "巡查商户")
    private Long patrolMerchants;
    @Schema(description = "巡查百分比")
    private Double patrolPercent;
    @Schema(description = "access商户")
    private Long accessMerchants;
    @Schema(description = "access设备")
    private Long accessDevices;
    @Schema(description = "设备总数")
    private Long deviceTotal;
    @Schema(description = "设备正常")
    private Long deviceNormal;
    @Schema(description = "设备报警")
    private Long deviceAlarm;
    @Schema(description = "设备故障")
    private Long deviceFault;
    @Schema(description = "设备离线")
    private Long deviceOffline;
    @Schema(description = "正常百分比")
    private Double normalPercent;
    @Schema(description = "报警百分比")
    private Double alarmPercent;
    @Schema(description = "故障百分比")
    private Double faultPercent;
    @Schema(description = "离线百分比")
    private Double offlinePercent;
    @Schema(description = "商户点位")
    private java.util.List<MerchantStatusPointVO> merchantPoints;
}
