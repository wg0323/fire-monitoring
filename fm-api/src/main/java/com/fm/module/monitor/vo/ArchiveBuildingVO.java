package com.fm.module.monitor.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 电子档案树：楼宇节点。
 */
@Data
@Schema(description = "电子档案树：楼宇节点")
public class ArchiveBuildingVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "楼宇主键ID")
    private Long buildingId;
    @Schema(description = "楼宇名称")
    private String buildingName;
    @Schema(description = "楼层")
    private List<ArchiveFloorVO> floors;
    @Schema(description = "设备")
    private List<MonitorDeviceVO> devices;
}
