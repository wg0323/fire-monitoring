package com.fm.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商户楼宇。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("b_merchant_building")
public class MerchantBuilding extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long merchantId;
    private String name;
    private Integer sort;
    private String remark;
    private Integer status;
}
