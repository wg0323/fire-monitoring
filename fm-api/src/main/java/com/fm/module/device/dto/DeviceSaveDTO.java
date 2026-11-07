package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备新增/修改请求。
 */
@Schema(description = "设备新增/修改请求")
@Data
public class DeviceSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "租户 ID")
    private Long tenantId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "设备 SN")
    private String sn;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "企业 ID")
    private Long merchantId;
    @Schema(description = "厂商")
    private String maker;
    @Schema(description = "具体位置")
    private String position;
    @Schema(description = "经度")
    private Double lng;
    @Schema(description = "纬度")
    private Double lat;
    @Schema(description = "位置类型")
    private Integer locationType;
    @Schema(description = "楼宇 ID")
    private Long buildingId;
    @Schema(description = "楼层 ID")
    private Long floorId;
    @Schema(description = "楼层坐标 X")
    private BigDecimal floorX;
    @Schema(description = "楼层坐标 Y")
    private BigDecimal floorY;
    @Schema(description = "SIM 卡号")
    private String sim;
    @Schema(description = "卡号")
    private String cardNo;
    @Schema(description = "通道号")
    private Integer channelNum;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "启用日期")
    private LocalDateTime activeDate;
    @Schema(description = "到期日期")
    private LocalDateTime expireDate;
    @Schema(description = "安装日期")
    private LocalDateTime installDate;
    @Schema(description = "扩展字段")
    private String fExt;
}
