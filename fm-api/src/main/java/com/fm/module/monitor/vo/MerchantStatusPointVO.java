package com.fm.module.monitor.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大屏地图商户点：状态按 火警 > 故障 > 离线 > 正常 取值。
 */
@Data
@Schema(description = "大屏地图商户点")
public class MerchantStatusPointVO {

    @Schema(description = "商户 id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long merchantId;

    @Schema(description = "商户名称")
    private String name;

    @Schema(description = "经度")
    private Double lng;

    @Schema(description = "纬度")
    private Double lat;

    @Schema(description = "状态：0正常 1火警 2故障 3离线 4未接入")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusText;

    @Schema(description = "商户场所照片 URL")
    private String picUrl;

    @Schema(description = "商户类型代码")
    private String type;

    @Schema(description = "商户类型名称")
    private String typeName;

    @Schema(description = "最近一次未处理报警时间")
    private LocalDateTime alarmTime;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "联系电话")
    private String phone;
}
