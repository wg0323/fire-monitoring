package com.fm.module.device.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商户详情中的楼层节点。
 */
@Data
@Schema(description = "商户详情中的楼层节点")
public class FloorNodeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "楼层否")
    private Integer floorNo;
    @Schema(description = "楼层计划链接地址")
    private String floorPlanUrl;
    @Schema(description = "计划width")
    private Integer planWidth;
    @Schema(description = "计划height")
    private Integer planHeight;
    @Schema(description = "设备数量")
    private Long deviceCount;
}
