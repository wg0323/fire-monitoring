package com.fm.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色，data_scope 决定行级数据范围。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final String SCOPE_ALL = "ALL";
    public static final String SCOPE_TENANT = "TENANT";
    public static final String SCOPE_WORK_AREA = "WORK_AREA";
    public static final String SCOPE_MERCHANT = "MERCHANT";

    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    public static final String TENANT_ADMIN = "TENANT_ADMIN";
    public static final String TENANT_DUTY = "TENANT_DUTY";
    public static final String TENANT_FIELD = "TENANT_FIELD";
    public static final String MERCHANT = "MERCHANT";

    private Long tenantId;
    private String roleName;
    private String roleKey;
    private String dataScope;
    private Integer status;
}
