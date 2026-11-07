package com.fm.module.device.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 楼层展示对象。
 */
@Data
@Schema(description = "楼层展示对象")
public class FloorVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "楼宇主键ID")
    private Long buildingId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "商户主键ID")
    private Long merchantId;
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
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "楼宇名称")
    private String buildingName;
    @Schema(description = "是否计划")
    private Boolean hasPlan;
    @Schema(description = "设备数量")
    private Long deviceCount;
}
