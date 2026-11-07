package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设备监控大屏：按设备类型的状态分布。
 */
@Data
@Schema(description = "设备监控大屏：按设备类型的状态分布")
public class ScreenDeviceTypeStatVO {

    @Schema(description = "类型")
    private String type;
    @Schema(description = "类型名称")
    private String typeName;
    @Schema(description = "总数")
    private Long total;
    @Schema(description = "正常")
    private Long normal;
    @Schema(description = "报警")
    private Long alarm;
    @Schema(description = "故障")
    private Long fault;
    @Schema(description = "离线")
    private Long offline;
    @Schema(description = "正常百分比")
    private Double normalPercent;
    @Schema(description = "报警百分比")
    private Double alarmPercent;
    @Schema(description = "故障百分比")
    private Double faultPercent;
    @Schema(description = "离线百分比")
    private Double offlinePercent;
}
