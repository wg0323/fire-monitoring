package com.fm.module.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色展示对象。
 */
@Data
@Schema(description = "角色展示对象")
public class RoleVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @Schema(description = "租户名称")
    private String tenantName;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "角色键")
    private String roleKey;
    @Schema(description = "数据scope")
    private String dataScope;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "用户数量")
    private Long userCount;
}
