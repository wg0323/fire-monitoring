package com.fm.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.system.dto.RoleQueryDTO;
import com.fm.module.system.dto.RoleSaveDTO;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.vo.RoleVO;

import java.util.List;

/**
 * 角色服务：平台只维护 tenant 0，租户管理员只维护本租户。
 */
public interface SysRoleService extends IService<SysRole> {

    Page<RoleVO> page(RoleQueryDTO query);

    List<RoleVO> list(Long tenantId, String userType);

    Long create(RoleSaveDTO request);

    void update(Long id, RoleSaveDTO request);

    void delete(Long id);

    List<Long> menuIds(Long roleId);

    void assignMenus(Long roleId, List<Long> menuIds);
}
