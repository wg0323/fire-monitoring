package com.fm.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.system.converter.RoleConverter;
import com.fm.module.system.dto.RoleQueryDTO;
import com.fm.module.system.dto.RoleSaveDTO;
import com.fm.module.system.entity.SysMenu;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysRoleMenu;
import com.fm.module.system.entity.SysUserRole;
import com.fm.module.system.mapper.SysMenuMapper;
import com.fm.module.system.mapper.SysOrgMapper;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysRoleMenuMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.service.SysRoleService;
import com.fm.module.system.support.AdminGuard;
import com.fm.module.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 角色维护：五类 roleKey，数据范围由 roleKey 推导。
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private static final long PLATFORM_TENANT_ID = 0L;
    private static final String PLATFORM_NAME = "平台";
    private static final Set<String> TENANT_ROLE_KEYS = new HashSet<>(Arrays.asList(
            SysRole.TENANT_ADMIN, SysRole.TENANT_DUTY, SysRole.TENANT_FIELD, SysRole.MERCHANT));
    private static final Set<String> PLATFORM_ROLE_KEYS = Collections.singleton(SysRole.PLATFORM_ADMIN);

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysOrgMapper orgMapper;

    @Override
    public Page<RoleVO> page(RoleQueryDTO query) {
        AdminGuard.requireAdmin();
        Long tenantId = currentTenantId();
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .like(StringUtils.hasText(query.getKeyword()), SysRole::getRoleName, query.getKeyword())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getId);
        Page<SysRole> entityPage = baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        Page<RoleVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<RoleVO> records = entityPage.getRecords().stream()
                .map(RoleConverter::toVO).collect(Collectors.toList());
        fillTenantNames(records);
        fillUserCounts(records);
        result.setRecords(records);
        return result;
    }

    @Override
    public List<RoleVO> list(Long tenantId, String userType) {
        AdminGuard.requireAdmin();
        Long targetTenant = targetTenantId(tenantId);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, targetTenant)
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId);
        Set<String> allowedKeys = allowedRoleKeysByUserType(userType);
        if (!allowedKeys.isEmpty()) {
            wrapper.in(SysRole::getRoleKey, allowedKeys);
        }
        List<SysRole> roles = queryIgnoringTenant(wrapper);
        List<RoleVO> records = roles.stream().map(RoleConverter::toVO).collect(Collectors.toList());
        fillTenantNames(records);
        return records;
    }

    @Override
    @Transactional
    public Long create(RoleSaveDTO request) {
        AdminGuard.requireAdmin();
        validate(request);
        Set<String> allowedKeys = AdminGuard.isPlatform() ? PLATFORM_ROLE_KEYS : TENANT_ROLE_KEYS;
        if (!allowedKeys.contains(request.getRoleKey())) {
            throw new BizException(403, "当前账号不能创建该角色");
        }
        Long tenantId = currentTenantId();
        Long exists = baseMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleKey, request.getRoleKey()));
        if (exists != null && exists > 0) {
            throw new BizException("角色标识已存在");
        }
        SysRole role = new SysRole();
        role.setTenantId(tenantId);
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setDataScope(scopeOf(request.getRoleKey()));
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        baseMapper.insert(role);
        if (request.getMenuIds() != null) {
            assignMenusInternal(role, request.getMenuIds());
        }
        return role.getId();
    }

    @Override
    @Transactional
    public void update(Long id, RoleSaveDTO request) {
        AdminGuard.requireAdmin();
        SysRole existing = requireAccessible(id);
        validate(request);
        if (!existing.getRoleKey().equals(request.getRoleKey())) {
            throw new BizException("角色标识不允许修改");
        }
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(request.getRoleName());
        role.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        baseMapper.updateById(role);
        if (request.getMenuIds() != null) {
            assignMenusInternal(existing, request.getMenuIds());
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AdminGuard.requireAdmin();
        SysRole role = requireAccessible(id);
        if (SysRole.PLATFORM_ADMIN.equals(role.getRoleKey()) || SysRole.TENANT_ADMIN.equals(role.getRoleKey())) {
            throw new BizException("内置管理员角色不能删除");
        }
        Long users = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (users != null && users > 0) {
            throw new BizException("角色下还有用户，不能删除");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        // 角色是配置数据，物理删除，避免软删行占用 (tenant_id, role_key) 唯一键
        baseMapper.physicalDeleteById(id);
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        AdminGuard.requireAdmin();
        requireAccessible(roleId);
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        AdminGuard.requireAdmin();
        SysRole role = requireAccessible(roleId);
        assignMenusInternal(role, menuIds);
    }

    private void assignMenusInternal(SysRole role, List<Long> menuIds) {
        List<Long> targetIds = menuIds == null ? Collections.emptyList()
                : menuIds.stream().distinct().collect(Collectors.toList());
        if (!targetIds.isEmpty()) {
            List<SysMenu> menus = menuMapper.selectBatchIds(targetIds);
            if (menus.size() != targetIds.size()) {
                throw new BizException(403, "存在不属于当前租户的菜单");
            }
            boolean crossTenant = menus.stream()
                    .anyMatch(menu -> !role.getTenantId().equals(menu.getTenantId()));
            if (crossTenant) {
                throw new BizException(403, "存在不属于当前租户的菜单");
            }
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, role.getId()));
        for (Long menuId : targetIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(role.getId());
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }

    private SysRole requireAccessible(Long id) {
        SysRole role = baseMapper.selectById(id);
        if (role == null) {
            throw new BizException("角色不存在或无权操作");
        }
        if (!role.getTenantId().equals(currentTenantId())) {
            throw new BizException(403, "不能维护其他租户的角色");
        }
        return role;
    }

    private void validate(RoleSaveDTO request) {
        if (!StringUtils.hasText(request.getRoleName())) {
            throw new BizException("角色名称不能为空");
        }
        if (!StringUtils.hasText(request.getRoleKey())) {
            throw new BizException("角色标识不能为空");
        }
    }

    private Long targetTenantId(Long requested) {
        if (AdminGuard.isPlatform()) {
            return requested == null ? PLATFORM_TENANT_ID : requested;
        }
        return TenantContext.getTenantId();
    }

    private Set<String> allowedRoleKeysByUserType(String userType) {
        if (!StringUtils.hasText(userType)) {
            return Collections.emptySet();
        }
        if ("PLATFORM".equals(userType)) {
            return PLATFORM_ROLE_KEYS;
        }
        if ("MERCHANT".equals(userType)) {
            return Collections.singleton(SysRole.MERCHANT);
        }
        if ("TENANT".equals(userType)) {
            return new HashSet<>(Arrays.asList(
                    SysRole.TENANT_ADMIN, SysRole.TENANT_DUTY, SysRole.TENANT_FIELD));
        }
        return Collections.emptySet();
    }

    private String scopeOf(String roleKey) {
        if (SysRole.PLATFORM_ADMIN.equals(roleKey)) {
            return SysRole.SCOPE_ALL;
        }
        if (SysRole.TENANT_FIELD.equals(roleKey)) {
            return SysRole.SCOPE_WORK_AREA;
        }
        if (SysRole.MERCHANT.equals(roleKey)) {
            return SysRole.SCOPE_MERCHANT;
        }
        return SysRole.SCOPE_TENANT;
    }

    private Long currentTenantId() {
        return AdminGuard.isPlatform() ? PLATFORM_TENANT_ID : TenantContext.getTenantId();
    }

    private List<SysRole> queryIgnoringTenant(LambdaQueryWrapper<SysRole> wrapper) {
        if (!AdminGuard.isPlatform()) {
            return baseMapper.selectList(wrapper);
        }
        return withIgnoreTenant(() -> baseMapper.selectList(wrapper));
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

    private void fillTenantNames(List<RoleVO> records) {
        List<Long> tenantIds = records.stream().map(RoleVO::getTenantId)
                .filter(id -> id != null && id != PLATFORM_TENANT_ID)
                .distinct().collect(Collectors.toList());
        Map<Long, String> names = tenantIds.isEmpty() ? Collections.emptyMap()
                : orgMapper.selectBatchIds(tenantIds).stream()
                .collect(Collectors.toMap(SysOrg::getId, SysOrg::getName, (a, b) -> a));
        for (RoleVO record : records) {
            Long tenantId = record.getTenantId();
            record.setTenantName(tenantId == null || tenantId == PLATFORM_TENANT_ID
                    ? PLATFORM_NAME : names.get(tenantId));
        }
    }

    private void fillUserCounts(List<RoleVO> records) {
        List<Long> roleIds = records.stream().map(RoleVO::getId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return;
        }
        Map<Long, Long> counts = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .in(SysUserRole::getRoleId, roleIds))
                .stream().collect(Collectors.groupingBy(SysUserRole::getRoleId, Collectors.counting()));
        for (RoleVO record : records) {
            record.setUserCount(counts.getOrDefault(record.getId(), 0L));
        }
    }
}
