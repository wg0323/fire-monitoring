package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 报警监控大屏：报警趋势、实时滚动与地图点。
 */
@Data
@Schema(description = "报警监控大屏：报警趋势、实时滚动与地图点")
public class ScreenAlarmVO {

    @Schema(description = "趋势")
    private List<ScreenTrendPointVO> trend;
    @Schema(description = "报警")
    private List<ScreenAlarmItemVO> alarms;
    @Schema(description = "商户点位")
    private List<MerchantStatusPointVO> merchantPoints;
}
