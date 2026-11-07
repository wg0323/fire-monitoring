package com.fm.module.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点。
 */
@Data
@Schema(description = "菜单树节点")
public class MenuVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "父级主键ID")
    private Long parentId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "menu键")
    private String menuKey;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "路径")
    private String path;
    @Schema(description = "字段 perms")
    private String perms;
    @Schema(description = "字段 seq")
    private Integer seq;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "字段 platformOnly")
    private Integer platformOnly;
    @Schema(description = "子菜单列表")
    private List<MenuVO> children = new ArrayList<>();
}
