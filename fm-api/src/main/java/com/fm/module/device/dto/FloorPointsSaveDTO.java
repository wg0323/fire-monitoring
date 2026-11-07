package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 楼层平面图点位批量保存请求。
 */
@Schema(description = "楼层平面图点位批量保存请求")
@Data
public class FloorPointsSaveDTO {

    @Schema(description = "点位列表")
    private List<Point> points;
    @Schema(description = "移除的设备 ID 列表")
    private List<Long> removedDeviceIds;

    /**
     * 单个设备点位，坐标为百分比。
     */
    @Data
    public static class Point {

        @Schema(description = "设备 ID")
        private Long deviceId;
        @Schema(description = "点位 X 坐标")
        private BigDecimal x;
        @Schema(description = "点位 Y 坐标")
        private BigDecimal y;
    }
}
