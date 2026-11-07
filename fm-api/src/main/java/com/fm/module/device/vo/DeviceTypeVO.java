package com.fm.module.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备类型字典项，含离线阈值与大屏显示配置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "设备类型字典项，含离线阈值与大屏显示配置")
public class DeviceTypeVO extends DictVO {

    @Schema(description = "离线threshold")
    private Integer offlineThreshold;
    @Schema(description = "离线单位")
    private String offlineUnit;
    @Schema(description = "监控标志")
    private Integer monitorFlag;
    @Schema(description = "排序")
    private Integer sort;
}
