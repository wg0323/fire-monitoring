package com.fm.module.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户展示对象（不含密码）。
 */
@Data
@Schema(description = "用户展示对象（不含密码）")
public class UserVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @Schema(description = "租户名称")
    private String tenantName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "部门主键ID")
    private Long deptId;
    @Schema(description = "部门名称")
    private String deptName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "商户主键ID")
    private Long merchantId;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "用户类型")
    private String userType;
    @Schema(description = "用户名称")
    private String userName;
    @Schema(description = "真实名称")
    private String realName;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "最后login")
    private LocalDateTime lastLogin;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "角色主键ID")
    private Long roleId;
    @Schema(description = "角色键")
    private String roleKey;
    @Schema(description = "角色名称")
    private String roleName;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    @Schema(description = "工作区域主键ID列表")
    private List<Long> workAreaIds;
    @Schema(description = "工作区域名称列表")
    private List<String> workAreaNames;
}
