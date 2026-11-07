package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商户新增/修改请求。
 */
@Schema(description = "商户新增/修改请求")
@Data
public class MerchantSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "企业性质")
    private Integer merchantKind;
    @Schema(description = "经度")
    private Double lng;
    @Schema(description = "纬度")
    private Double lat;
    @Schema(description = "地址")
    private String addr;
    @Schema(description = "所属区域")
    private String area;
    @Schema(description = "负责人姓名")
    private String ownerName;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "经营范围")
    private String businessScope;
    @Schema(description = "经营内容")
    private String businessContent;
    @Schema(description = "建筑面积")
    private String floorage;
    @Schema(description = "员工人数")
    private Integer employeeNum;
    @Schema(description = "可容纳人数")
    private Integer allowNum;
    @Schema(description = "营业时间")
    private String businessHours;
    @Schema(description = "周边环境")
    private String roundEnv;
    @Schema(description = "是否配置场所照片")
    private Integer placeFlag;
    @Schema(description = "联系人 1")
    private String contact1;
    @Schema(description = "联系电话 1")
    private String phone1;
    @Schema(description = "联系人 2")
    private String contact2;
    @Schema(description = "联系电话 2")
    private String phone2;
    @Schema(description = "营业执照地址")
    private String businessLicense;
    @Schema(description = "消防备注")
    private String fireRemark;
    @Schema(description = "场所照片 1")
    private String placePic1;
    @Schema(description = "场所照片 2")
    private String placePic2;
    @Schema(description = "场所照片 3")
    private String placePic3;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "是否投保")
    private Integer insureFlag;
    @Schema(description = "状态")
    private Integer status;
}
