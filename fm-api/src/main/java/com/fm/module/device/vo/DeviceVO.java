package com.fm.module.device.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备展示对象。
 */
@Data
@Schema(description = "设备展示对象")
public class DeviceVO {

    /** 主键ID */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    /** 租户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "设备编号")
    private String sn;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "类型名称")
    private String typeName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "商户主键ID")
    private Long merchantId;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "制造商")
    private String maker;
    @Schema(description = "制造商名称")
    private String makerName;
    @Schema(description = "位置")
    private String position;
    @Schema(description = "经度")
    private Double lng;
    @Schema(description = "纬度")
    private Double lat;
    @Schema(description = "定位类型")
    private Integer locationType;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "楼宇主键ID")
    private Long buildingId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "楼层主键ID")
    private Long floorId;
    @Schema(description = "楼层X坐标")
    private BigDecimal floorX;
    @Schema(description = "楼层Y坐标")
    private BigDecimal floorY;
    @Schema(description = "楼宇名称")
    private String buildingName;
    @Schema(description = "楼层名称")
    private String floorName;
    @Schema(description = "定位文本")
    private String locationText;
    @Schema(description = "SIM卡号")
    private String sim;
    @Schema(description = "卡号否")
    private String cardNo;
    @Schema(description = "通道数量")
    private Integer channelNum;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "在线状态")
    private Integer onlineStatus;
    @Schema(description = "最后时间")
    private LocalDateTime lastTime;
    @Schema(description = "激活日期")
    private LocalDateTime activeDate;
    @Schema(description = "到期日期")
    private LocalDateTime expireDate;
    @Schema(description = "安装日期")
    private LocalDateTime installDate;
    @Schema(description = "字段 fExt")
    private String fExt;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
