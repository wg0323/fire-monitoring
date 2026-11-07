package com.fm.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商户楼层，含平面图信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("b_merchant_floor")
public class MerchantFloor extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long buildingId;
    private Long merchantId;
    private String name;
    private Integer floorNo;
    private String floorPlanUrl;
    private Integer planWidth;
    private Integer planHeight;
    private Integer sort;
    private Integer status;
}
