package com.fm.module.device.vo;

import com.fm.module.monitor.vo.MonitorTrendVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 后台设备详情：基础信息 + 实时快照 + 趋势 + 最近报警。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台设备详情：基础信息 + 实时快照 + 趋势 + 最近报警")
public class DeviceDetailVO extends DeviceVO {

    @Schema(description = "实时快照指标列表")
    private List<DeviceSnapshotItemVO> snapshot = new ArrayList<>();
    @Schema(description = "历史趋势序列列表")
    private List<MonitorTrendVO> trends = new ArrayList<>();
    @Schema(description = "最近报警列表")
    private List<DeviceAlarmBriefVO> recentAlarms = new ArrayList<>();
}
