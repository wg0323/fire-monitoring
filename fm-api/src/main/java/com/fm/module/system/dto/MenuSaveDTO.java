package com.fm.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 菜单新增/修改请求，tenant_id 不在请求体中传递。
 */
@Schema(description = "菜单新增/修改请求，tenant_id 不在请求体中传递")
@Data
public class MenuSaveDTO {

    @Schema(description = "上级 ID")
    private Long parentId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "菜单标识")
    private String menuKey;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "路由路径")
    private String path;
    @Schema(description = "权限标识")
    private String perms;
    @Schema(description = "排序号")
    private Integer seq;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "是否仅平台可用")
    private Integer platformOnly;
}
