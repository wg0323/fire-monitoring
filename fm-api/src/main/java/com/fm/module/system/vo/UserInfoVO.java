package com.fm.module.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fm.module.device.vo.MerchantOptionVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息。
 */
@Data
@Schema(description = "当前登录用户信息")
public class UserInfoVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "用户名称")
    private String userName;
    @Schema(description = "真实名称")
    private String realName;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "用户类型")
    private String userType;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "部门主键ID")
    private Long deptId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "商户主键ID")
    private Long merchantId;
    @Schema(description = "租户名称")
    private String tenantName;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "商户")
    private List<MerchantOptionVO> merchants;
    @Schema(description = "工作区域名称列表")
    private List<String> workAreaNames;
    @Schema(description = "数据scope")
    private String dataScope;
    @Schema(description = "字段 wxBound")
    private Boolean wxBound;
    @Schema(description = "角色")
    private List<String> roles;
    @Schema(description = "字段 permissions")
    private List<String> permissions;
    @Schema(description = "字段 menus")
    private List<MenuVO> menus;
}
