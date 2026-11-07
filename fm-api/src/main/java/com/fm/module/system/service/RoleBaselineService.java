package com.fm.module.system.service;

/**
 * 租户角色基线：新建运营中心时创建四类租户角色并分配菜单。
 */
public interface RoleBaselineService {

    /**
     * 为新租户创建 TENANT_ADMIN / TENANT_DUTY / TENANT_FIELD / MERCHANT 四类角色。
     * 已存在的角色不重复创建；角色无菜单时才补授权。
     *
     * @param tenantId 新租户 id
     * @return 新建角色数
     */
    int provisionTenantRoles(Long tenantId);
}
