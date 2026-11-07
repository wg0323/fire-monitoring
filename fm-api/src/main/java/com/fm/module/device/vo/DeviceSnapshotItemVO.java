package com.fm.module.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备实时快照指标项。
 */
@Data
@Schema(description = "设备实时快照指标项")
public class DeviceSnapshotItemVO {

    @Schema(description = "标签")
    private String label;
    @Schema(description = "数值")
    private String value;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "时间")
    private LocalDateTime time;
}
