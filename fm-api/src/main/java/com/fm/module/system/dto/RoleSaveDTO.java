package com.fm.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 角色新增/修改请求，数据范围由角色标识推导。
 */
@Schema(description = "角色新增/修改请求，数据范围由角色标识推导")
@Data
public class RoleSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "角色标识")
    private String roleKey;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "菜单 ID 列表")
    private List<Long> menuIds;
}
