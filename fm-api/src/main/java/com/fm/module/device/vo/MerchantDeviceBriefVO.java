package com.fm.module.device.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户详情中的设备摘要。
 */
@Data
@Schema(description = "商户详情中的设备摘要")
public class MerchantDeviceBriefVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "设备编号")
    private String sn;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "类型名称")
    private String typeName;
    @Schema(description = "位置")
    private String position;
    @Schema(description = "在线状态")
    private Integer onlineStatus;
    @Schema(description = "最后时间")
    private LocalDateTime lastTime;
    @Schema(description = "定位文本")
    private String locationText;
}
