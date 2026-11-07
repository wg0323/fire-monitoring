package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 楼层平面图保存请求。
 */
@Schema(description = "楼层平面图保存请求")
@Data
public class FloorPlanDTO {

    @Schema(description = "平面图地址")
    private String planUrl;
    @Schema(description = "平面图宽度")
    private Integer planWidth;
    @Schema(description = "平面图高度")
    private Integer planHeight;
}
