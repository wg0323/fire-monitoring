package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 楼层新增/修改请求。
 */
@Schema(description = "楼层新增/修改请求")
@Data
public class FloorSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "楼宇 ID")
    private Long buildingId;
    @Schema(description = "企业 ID")
    private Long merchantId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "楼层号")
    private Integer floorNo;
    @Schema(description = "楼层平面图地址")
    private String floorPlanUrl;
    @Schema(description = "平面图宽度")
    private Integer planWidth;
    @Schema(description = "平面图高度")
    private Integer planHeight;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态")
    private Integer status;
}
