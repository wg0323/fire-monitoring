package com.fm.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商户：P2 补全基础档案字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("b_merchant")
public class Merchant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private String name;
    private String type;
    private Integer merchantKind;
    private Double lng;
    private Double lat;
    private String addr;
    private String area;
    private String ownerName;
    private String phone;
    private String businessScope;
    private String businessContent;
    private String floorage;
    private Integer employeeNum;
    private Integer allowNum;
    private String businessHours;
    private String roundEnv;
    private Integer placeFlag;
    private String contact1;
    private String phone1;
    private String contact2;
    private String phone2;
    private String businessLicense;
    private String fireRemark;
    private String placePic1;
    private String placePic2;
    private String placePic3;
    private String remark;
    private Integer insureFlag;
    private String wxOpenId;
    private String regCode;
    private Integer status;
}
