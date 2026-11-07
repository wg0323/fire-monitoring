package com.fm.module.device.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商户展示对象。
 */
@Data
@Schema(description = "商户展示对象")
public class MerchantVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "类型名称")
    private String typeName;
    @Schema(description = "商户类别")
    private Integer merchantKind;
    @Schema(description = "经度")
    private Double lng;
    @Schema(description = "纬度")
    private Double lat;
    @Schema(description = "地址")
    private String addr;
    @Schema(description = "区域")
    private String area;
    @Schema(description = "owner名称")
    private String ownerName;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "字段 businessScope")
    private String businessScope;
    @Schema(description = "business内容")
    private String businessContent;
    @Schema(description = "字段 floorage")
    private String floorage;
    @Schema(description = "employee数量")
    private Integer employeeNum;
    @Schema(description = "allow数量")
    private Integer allowNum;
    @Schema(description = "business小时")
    private String businessHours;
    @Schema(description = "字段 roundEnv")
    private String roundEnv;
    @Schema(description = "place标志")
    private Integer placeFlag;
    @Schema(description = "字段 contact1")
    private String contact1;
    @Schema(description = "字段 phone1")
    private String phone1;
    @Schema(description = "字段 contact2")
    private String contact2;
    @Schema(description = "字段 phone2")
    private String phone2;
    @Schema(description = "字段 businessLicense")
    private String businessLicense;
    @Schema(description = "火警备注")
    private String fireRemark;
    @Schema(description = "字段 placePic1")
    private String placePic1;
    @Schema(description = "字段 placePic2")
    private String placePic2;
    @Schema(description = "字段 placePic3")
    private String placePic3;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "insure标志")
    private Integer insureFlag;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "工作区域名称列表")
    private List<String> workAreaNames;
    @Schema(description = "设备数量")
    private Long deviceCount;
    @Schema(description = "login账号")
    private String loginAccount;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
