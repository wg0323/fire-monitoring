package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 设备监控大屏：类型统计、故障趋势、故障滚动与地图点。
 */
@Data
@Schema(description = "设备监控大屏：类型统计、故障趋势、故障滚动与地图点")
public class ScreenDeviceVO {

    @Schema(description = "类型")
    private List<ScreenDeviceTypeStatVO> types;
    @Schema(description = "故障趋势")
    private List<ScreenTrendPointVO> faultTrend;
    @Schema(description = "故障")
    private List<ScreenFaultItemVO> faults;
    @Schema(description = "商户点位")
    private List<MerchantStatusPointVO> merchantPoints;
}
