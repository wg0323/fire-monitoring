package com.fm.module.system.seed;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fm.common.context.TenantContext;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.system.entity.SysDept;
import com.fm.module.system.entity.SysMenu;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysRoleMenu;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysUserRole;
import com.fm.module.system.entity.SysUserWorkArea;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysDeptMapper;
import com.fm.module.system.mapper.SysMenuMapper;
import com.fm.module.system.mapper.SysOrgMapper;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysRoleMenuMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.MenuBaselineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * P1 种子数据：2 个租户、五类角色、完整菜单基线与 8 个演示账号。
 * 启用方式：--spring.profiles.active=p1seed；仅在 fm_p1 空库使用。
 */
@Slf4j
@Component
@Profile("p1seed")
@RequiredArgsConstructor
public class SeedDataRunner implements ApplicationRunner {

    private static final long PLATFORM_TENANT_ID = 0L;
    private static final long TENANT_A = 1L;
    private static final long TENANT_B = 2L;
    private static final String DEFAULT_PASSWORD = "123456";

    private static final Set<String> DUTY_DIR_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "screen", "duty", "trace", "workorder", "archive", "service", "stat", "report")));
    private static final Set<String> DUTY_PAGE_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "screen:comprehensive", "screen:alarm", "screen:danger", "screen:device",
            "duty:fire", "duty:danger",
            "trace:alarm", "trace:fire", "trace:danger", "workorder:list",
            "archive:water", "archive:electric",
            "service:merchant", "service:device", "service:inspection",
            "stat:comprehensive", "stat:alarm", "stat:fault", "stat:danger", "stat:fire", "stat:call",
            "report:month", "report:year")));

    /** 值守在 P2 需要的处置按钮权限 */
    private static final Set<String> DUTY_BUTTON_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "alarm:verify", "alarm:dispatch", "alarm:call", "alarm:convert-danger",
            "danger:assign", "danger:audit")));

    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysDeptMapper deptMapper;
    private final SysWorkAreaMapper workAreaMapper;
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper;
    private final SysUserWorkAreaMapper userWorkAreaMapper;
    private final MerchantMapper merchantMapper;
    private final MenuBaselineService menuBaselineService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        TenantContext.set(PLATFORM_TENANT_ID, "PLATFORM", SysRole.SCOPE_ALL, true);
        try {
            seedOrgs();
            menuBaselineService.ensureBaseline();
            menuBaselineService.copyBaseline(TENANT_A);
            menuBaselineService.copyBaseline(TENANT_B);
            Map<String, Long> roles = seedRoles();
            seedRoleMenus(roles);
            seedDepts();
            seedMerchants();
            seedWorkAreas();
            seedUsers(roles);
            bindWorkAreaRelations();
            // 默认口令不在日志中输出：口令值本身保持不变，仅移除日志参数。
            // 不使用 LogMask.mask 处理纯口令——它只掩码 URL 查询参数与手机号，对裸口令是假脱敏。
            log.info("p1 seed ready: admin/cd_admin/cd_duty/cd_field/cd_merchant");
        } finally {
            TenantContext.clear();
        }
    }

    private void seedOrgs() {
        ensureOrg(TENANT_A, "昌都市", "CD");
        ensureOrg(TENANT_B, "江达县", "JD");
    }

    private void ensureOrg(Long id, String name, String code) {
        if (orgMapper.selectById(id) != null) {
            return;
        }
        SysOrg org = new SysOrg();
        org.setId(id);
        org.setType(SysOrg.TYPE_TENANT);
        org.setName(name);
        org.setCode(code);
        org.setStatus(1);
        orgMapper.insert(org);
    }

    private Map<String, Long> seedRoles() {
        Map<String, Long> roles = new HashMap<>();
        roles.put(roleKey(PLATFORM_TENANT_ID, SysRole.PLATFORM_ADMIN),
                ensureRole(9001L, PLATFORM_TENANT_ID, "平台管理员", SysRole.PLATFORM_ADMIN));
        roles.put(roleKey(TENANT_A, SysRole.TENANT_ADMIN),
                ensureRole(9101L, TENANT_A, "租户管理员", SysRole.TENANT_ADMIN));
        roles.put(roleKey(TENANT_A, SysRole.TENANT_DUTY),
                ensureRole(9102L, TENANT_A, "租户值守", SysRole.TENANT_DUTY));
        roles.put(roleKey(TENANT_A, SysRole.TENANT_FIELD),
                ensureRole(9103L, TENANT_A, "租户外勤", SysRole.TENANT_FIELD));
        roles.put(roleKey(TENANT_A, SysRole.MERCHANT),
                ensureRole(9104L, TENANT_A, "商户", SysRole.MERCHANT));
        roles.put(roleKey(TENANT_B, SysRole.TENANT_ADMIN),
                ensureRole(9201L, TENANT_B, "租户管理员", SysRole.TENANT_ADMIN));
        roles.put(roleKey(TENANT_B, SysRole.TENANT_DUTY),
                ensureRole(9202L, TENANT_B, "租户值守", SysRole.TENANT_DUTY));
        roles.put(roleKey(TENANT_B, SysRole.TENANT_FIELD),
                ensureRole(9203L, TENANT_B, "租户外勤", SysRole.TENANT_FIELD));
        roles.put(roleKey(TENANT_B, SysRole.MERCHANT),
                ensureRole(9204L, TENANT_B, "商户", SysRole.MERCHANT));
        return roles;
    }

    private Long ensureRole(Long id, Long tenantId, String name, String roleKey) {
        SysRole existing = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleKey, roleKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }
        SysRole role = new SysRole();
        role.setId(id);
        role.setTenantId(tenantId);
        role.setRoleName(name);
        role.setRoleKey(roleKey);
        role.setDataScope(scopeOf(roleKey));
        role.setStatus(1);
        roleMapper.insert(role);
        return role.getId();
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

    private void seedRoleMenus(Map<String, Long> roles) {
        assignRoleMenus(roles.get(roleKey(PLATFORM_TENANT_ID, SysRole.PLATFORM_ADMIN)), menuIds(PLATFORM_TENANT_ID));
        assignRoleMenus(roles.get(roleKey(TENANT_A, SysRole.TENANT_ADMIN)), menuIds(TENANT_A));
        assignRoleMenus(roles.get(roleKey(TENANT_B, SysRole.TENANT_ADMIN)), menuIds(TENANT_B));
        assignRoleMenus(roles.get(roleKey(TENANT_A, SysRole.TENANT_DUTY)), dutyMenuIds(TENANT_A));
        assignRoleMenus(roles.get(roleKey(TENANT_B, SysRole.TENANT_DUTY)), dutyMenuIds(TENANT_B));
    }

    private List<Long> menuIds(Long tenantId) {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getTenantId, tenantId))
                .stream().map(SysMenu::getId).collect(Collectors.toList());
    }

    private List<Long> dutyMenuIds(Long tenantId) {
        Set<String> keys = new HashSet<>(unionKeys());
        keys.addAll(DUTY_BUTTON_KEYS);
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getTenantId, tenantId)
                        .in(SysMenu::getMenuKey, keys))
                .stream().map(SysMenu::getId).collect(Collectors.toList());
    }

    private Set<String> unionKeys() {
        Set<String> keys = new HashSet<>(DUTY_DIR_KEYS);
        keys.addAll(DUTY_PAGE_KEYS);
        return keys;
    }

    private void assignRoleMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null) {
            return;
        }
        Set<Long> assigned = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
        for (Long menuId : menuIds) {
            if (assigned.contains(menuId)) {
                continue;
            }
            SysRoleMenu relation = new SysRoleMenu();
            relation.setId(IdWorker.getId());
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }

    private void seedDepts() {
        ensureDept(1L, TENANT_A, "昌都运营部");
        ensureDept(2L, TENANT_B, "江达运营部");
    }

    private void ensureDept(Long id, Long tenantId, String name) {
        if (deptMapper.selectById(id) != null) {
            return;
        }
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setTenantId(tenantId);
        dept.setParentId(0L);
        dept.setName(name);
        dept.setStatus(1);
        deptMapper.insert(dept);
    }

    private void seedMerchants() {
        ensureMerchant(1L, TENANT_A, "昌都示范商户");
        ensureMerchant(2L, TENANT_B, "江达示范商户");
    }

    private void ensureMerchant(Long id, Long tenantId, String name) {
        if (merchantMapper.selectById(id) != null) {
            return;
        }
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setTenantId(tenantId);
        merchant.setName(name);
        merchant.setStatus(1);
        merchantMapper.insert(merchant);
    }

    private void seedWorkAreas() {
        ensureWorkArea(1L, TENANT_A, "昌都城区");
        ensureWorkArea(2L, TENANT_B, "江达城区");
    }

    private void ensureWorkArea(Long id, Long tenantId, String name) {
        if (workAreaMapper.selectById(id) != null) {
            return;
        }
        SysWorkArea area = new SysWorkArea();
        area.setId(id);
        area.setTenantId(tenantId);
        area.setOrgId(tenantId);
        area.setName(name);
        area.setRemark("P1 种子数据");
        workAreaMapper.insert(area);
    }

    private void seedUsers(Map<String, Long> roles) {
        seedUser(1L, PLATFORM_TENANT_ID, null, "PLATFORM", "admin", "13800000000", "平台管理员",
                roles.get(roleKey(PLATFORM_TENANT_ID, SysRole.PLATFORM_ADMIN)), null);
        seedUser(1001L, TENANT_A, 1L, "TENANT", "cd_admin", "13800000001", "昌都管理员",
                roles.get(roleKey(TENANT_A, SysRole.TENANT_ADMIN)), null);
        seedUser(1002L, TENANT_A, 1L, "TENANT", "cd_duty", "13800000002", "昌都值守",
                roles.get(roleKey(TENANT_A, SysRole.TENANT_DUTY)), null);
        seedUser(1003L, TENANT_A, 1L, "TENANT", "cd_field", "13800000003", "昌都外勤",
                roles.get(roleKey(TENANT_A, SysRole.TENANT_FIELD)), null);
        seedUser(1004L, TENANT_A, 1L, "MERCHANT", "cd_merchant", "13800000004", "昌都商户",
                roles.get(roleKey(TENANT_A, SysRole.MERCHANT)), 1L);
        seedUser(1011L, TENANT_B, 2L, "TENANT", "jd_admin", "13800000011", "江达管理员",
                roles.get(roleKey(TENANT_B, SysRole.TENANT_ADMIN)), null);
        seedUser(1012L, TENANT_B, 2L, "TENANT", "jd_duty", "13800000012", "江达值守",
                roles.get(roleKey(TENANT_B, SysRole.TENANT_DUTY)), null);
        seedUser(1013L, TENANT_B, 2L, "TENANT", "jd_field", "13800000013", "江达外勤",
                roles.get(roleKey(TENANT_B, SysRole.TENANT_FIELD)), null);
        seedUser(1014L, TENANT_B, 2L, "MERCHANT", "jd_merchant", "13800000014", "江达商户",
                roles.get(roleKey(TENANT_B, SysRole.MERCHANT)), 2L);
    }

    private void seedUser(Long id, Long tenantId, Long deptId, String userType, String userName,
                          String phone, String realName, Long roleId, Long merchantId) {
        SysUser existing = userMapper.selectByUserName(userName);
        Long userId = existing == null ? id : existing.getId();
        if (existing == null) {
            SysUser user = new SysUser();
            user.setId(id);
            user.setTenantId(tenantId);
            user.setDeptId(deptId);
            user.setUserType(userType);
            user.setUserName(userName);
            user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            user.setRealName(realName);
            user.setPhone(phone);
            user.setMerchantId(merchantId);
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (roleId != null) {
            bindUserRole(userId, roleId);
        }
    }

    private void bindUserRole(Long userId, Long roleId) {
        Long exists = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId));
        if (exists != null && exists > 0) {
            return;
        }
        SysUserRole relation = new SysUserRole();
        relation.setId(IdWorker.getId());
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        userRoleMapper.insert(relation);
    }

    private void bindWorkAreaRelations() {
        Long cdField = userId("cd_field");
        Long jdField = userId("jd_field");
        if (cdField != null) {
            bindUserWorkArea(cdField, TENANT_A, 1L);
        }
        if (jdField != null) {
            bindUserWorkArea(jdField, TENANT_B, 2L);
        }
        bindWorkAreaMerchant(1L, TENANT_A, 1L);
        bindWorkAreaMerchant(2L, TENANT_B, 2L);
    }

    private Long userId(String userName) {
        SysUser user = userMapper.selectByUserName(userName);
        return user == null ? null : user.getId();
    }

    private void bindUserWorkArea(Long userId, Long tenantId, Long workAreaId) {
        Long exists = userWorkAreaMapper.selectCount(new LambdaQueryWrapper<SysUserWorkArea>()
                .eq(SysUserWorkArea::getUserId, userId)
                .eq(SysUserWorkArea::getWorkAreaId, workAreaId));
        if (exists != null && exists > 0) {
            return;
        }
        SysUserWorkArea relation = new SysUserWorkArea();
        relation.setId(IdWorker.getId());
        relation.setTenantId(tenantId);
        relation.setUserId(userId);
        relation.setWorkAreaId(workAreaId);
        userWorkAreaMapper.insert(relation);
    }

    private void bindWorkAreaMerchant(Long workAreaId, Long tenantId, Long merchantId) {
        Long exists = workAreaMerchantMapper.selectCount(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                .eq(SysWorkAreaMerchant::getWorkAreaId, workAreaId)
                .eq(SysWorkAreaMerchant::getMerchantId, merchantId));
        if (exists != null && exists > 0) {
            return;
        }
        SysWorkAreaMerchant relation = new SysWorkAreaMerchant();
        relation.setId(IdWorker.getId());
        relation.setTenantId(tenantId);
        relation.setWorkAreaId(workAreaId);
        relation.setMerchantId(merchantId);
        workAreaMerchantMapper.insert(relation);
    }

    private String roleKey(Long tenantId, String roleKey) {
        return tenantId + ":" + roleKey;
    }
}
