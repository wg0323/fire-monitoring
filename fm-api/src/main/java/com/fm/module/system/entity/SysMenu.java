package com.fm.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单：按 tenant_id 隔离，0 为平台基线，租户各持一份。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long parentId;
    private String name;
    private String menuKey;
    private Integer type;
    private String path;
    private String perms;
    private Integer seq;
    private Integer status;
    private Integer platformOnly;
}
