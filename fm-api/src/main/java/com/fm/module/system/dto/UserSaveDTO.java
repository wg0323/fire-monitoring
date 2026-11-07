package com.fm.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户新增/修改请求。
 */
@Schema(description = "用户新增/修改请求")
@Data
public class UserSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "租户 ID")
    private Long tenantId;
    @Schema(description = "部门 ID")
    private Long deptId;
    @Schema(description = "用户类型")
    private String userType;
    @Schema(description = "用户名")
    private String userName;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "企业 ID")
    private Long merchantId;
    @Schema(description = "角色 ID")
    private Long roleId;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "工作区 ID 列表")
    private List<Long> workAreaIds;
}
