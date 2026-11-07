package com.fm.module.device.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 楼层平面图布点：只返回绘制所需字段。
 */
@Data
@Schema(description = "楼层平面图布点：只返回绘制所需字段")
public class DevicePointVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "设备编号")
    private String sn;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "定位类型")
    private Integer locationType;
    @Schema(description = "楼层X坐标")
    private BigDecimal floorX;
    @Schema(description = "楼层Y坐标")
    private BigDecimal floorY;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "在线状态")
    private Integer onlineStatus;
}
