package com.fm.module.monitor.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 电子档案树：楼层节点。
 */
@Data
@Schema(description = "电子档案树：楼层节点")
public class ArchiveFloorVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "楼层主键ID")
    private Long floorId;
    @Schema(description = "楼层名称")
    private String floorName;
    @Schema(description = "设备")
    private List<MonitorDeviceVO> devices;
}
