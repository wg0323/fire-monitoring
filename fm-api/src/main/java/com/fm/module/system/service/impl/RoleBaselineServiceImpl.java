package com.fm.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.system.entity.SysMenu;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysRoleMenu;
import com.fm.module.system.mapper.SysMenuMapper;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysRoleMenuMapper;
import com.fm.module.system.service.RoleBaselineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 租户角色基线：与菜单复制同一事务执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleBaselineServiceImpl implements RoleBaselineService {

    private static final long PLATFORM_TENANT_ID = 0L;
    private static final String[] TENANT_ROLE_KEYS = {
            SysRole.TENANT_ADMIN, SysRole.TENANT_DUTY, SysRole.TENANT_FIELD, SysRole.MERCHANT
    };

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    @Override
    @Transactional
    public int provisionTenantRoles(Long tenantId) {
        if (tenantId == null || tenantId == PLATFORM_TENANT_ID) {
            throw new BizException("租户 id 不合法");
        }
        return withIgnoreTenant(() -> {
            List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getTenantId, tenantId)
                    .orderByAsc(SysMenu::getSeq).orderByAsc(SysMenu::getId));
            int created = 0;
            for (String roleKey : TENANT_ROLE_KEYS) {
                SysRole role = findRole(tenantId, roleKey);
                if (role == null) {
                    role = new SysRole();
                    role.setId(IdWorker.getId());
                    role.setTenantId(tenantId);
                    role.setRoleName(roleNameOf(roleKey));
                    role.setRoleKey(roleKey);
                    role.setDataScope(scopeOf(roleKey));
                    role.setStatus(1);
                    roleMapper.insert(role);
                    created++;
                }
                assignMenusIfEmpty(role, menuIdsFor(role, menus));
            }
            log.info("tenant {} roles provisioned, created={}", tenantId, created);
            return created;
        });
    }

    private SysRole findRole(Long tenantId, String roleKey) {
        return roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleKey, roleKey)
                .last("LIMIT 1"));
    }

    private void assignMenusIfEmpty(SysRole role, List<Long> menuIds) {
        Long exists = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, role.getId()));
        if (exists != null && exists > 0) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setId(IdWorker.getId());
            relation.setRoleId(role.getId());
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }

    private List<Long> menuIdsFor(SysRole role, List<SysMenu> menus) {
        if (SysRole.TENANT_ADMIN.equals(role.getRoleKey())) {
            return menus.stream().map(SysMenu::getId).collect(Collectors.toList());
        }
        if (SysRole.TENANT_DUTY.equals(role.getRoleKey())) {
            List<Long> result = new ArrayList<>();
            for (SysMenu menu : menus) {
                if (menu.getType() != null && menu.getType() == 2) {
                    continue;
                }
                String key = menu.getMenuKey() == null ? "" : menu.getMenuKey();
                if (key.startsWith("system") || key.startsWith("dict")) {
                    continue;
                }
                result.add(menu.getId());
            }
            return result;
        }
        return new ArrayList<>();
    }

    private String roleNameOf(String roleKey) {
        if (SysRole.TENANT_ADMIN.equals(roleKey)) {
            return "租户管理员";
        }
        if (SysRole.TENANT_DUTY.equals(roleKey)) {
            return "租户值守";
        }
        if (SysRole.TENANT_FIELD.equals(roleKey)) {
            return "租户外勤";
        }
        return "商户";
    }

    private String scopeOf(String roleKey) {
        if (SysRole.TENANT_FIELD.equals(roleKey)) {
            return SysRole.SCOPE_WORK_AREA;
        }
        if (SysRole.MERCHANT.equals(roleKey)) {
            return SysRole.SCOPE_MERCHANT;
        }
        return SysRole.SCOPE_TENANT;
    }

    private <T> T withIgnoreTenant(Supplier<T> action) {
        boolean previous = TenantContext.isIgnoreTenant();
        TenantContext.setIgnoreTenant(true);
        try {
            return action.get();
        } finally {
            TenantContext.setIgnoreTenant(previous);
        }
    }
}
