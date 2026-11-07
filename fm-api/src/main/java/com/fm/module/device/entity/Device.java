package com.fm.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备主表（瘦身版）：公共字段 + 三模式定位，扩展属性进 fExt。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("b_device")
public class Device extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final int LOCATION_NONE = 0;
    public static final int LOCATION_GPS = 1;
    public static final int LOCATION_FLOOR = 2;

    private Long tenantId;
    private String name;
    private String sn;
    private String type;
    private Long merchantId;
    private String maker;
    private String position;
    private Double lng;
    private Double lat;
    private Integer locationType;
    private Long buildingId;
    private Long floorId;
    private BigDecimal floorX;
    private BigDecimal floorY;
    private String sim;
    private String cardNo;
    private Integer channelNum;
    private Integer status;
    private Integer onlineStatus;
    private LocalDateTime lastTime;
    private LocalDateTime activeDate;
    private LocalDateTime expireDate;
    private LocalDateTime installDate;
    private String fExt;
}
