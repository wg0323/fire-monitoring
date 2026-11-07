package com.fm.module.system.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.vo.MerchantOptionVO;
import com.fm.module.system.converter.MenuConverter;
import com.fm.module.system.dto.LoginDTO;
import com.fm.module.system.dto.PasswordChangeDTO;
import com.fm.module.system.entity.LogLogin;
import com.fm.module.system.entity.SysMenu;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysRoleMenu;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysUserMerchant;
import com.fm.module.system.entity.SysUserRole;
import com.fm.module.system.entity.SysUserWorkArea;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.mapper.LogLoginMapper;
import com.fm.module.system.mapper.SysMenuMapper;
import com.fm.module.system.mapper.SysOrgMapper;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysRoleMenuMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserMerchantMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.service.AuthService;
import com.fm.module.system.converter.AuthConverter;
import com.fm.module.system.vo.LoginVO;
import com.fm.module.system.vo.MenuVO;
import com.fm.module.system.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 登录、会话与菜单。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String USER_TYPE_PLATFORM = "PLATFORM";
    private static final String USER_TYPE_TENANT = "TENANT";
    private static final String USER_TYPE_MERCHANT = "MERCHANT";
    private static final String PERMISSION_ALL = "*";
    private static final String PLATFORM_NAME = "平台";
    private static final String SESSION_TENANT_ID = "tenantId";
    private static final String SESSION_USER_TYPE = "userType";
    private static final String SESSION_DATA_SCOPE = "dataScope";
    private static final String SESSION_MERCHANT_ID = "merchantId";
    private static final String SESSION_MERCHANT_IDS = "merchantIds";
    private static final String SESSION_ROLE_KEYS = "roleKeys";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMerchantMapper userMerchantMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysOrgMapper orgMapper;
    private final MerchantMapper merchantMapper;
    private final SysUserWorkAreaMapper userWorkAreaMapper;
    private final SysWorkAreaMapper workAreaMapper;
    private final LogLoginMapper logLoginMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO request, String ip, String device, Set<String> allowedRoleKeys) {
        // 登录前没有租户上下文，忽略租户插件查全库
        TenantContext.set(0L, USER_TYPE_PLATFORM, SysRole.SCOPE_ALL, true);
        try {
            SysUser user = userMapper.selectByUserName(request.getUserName());
            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                recordLogin(user, ip, 0, "用户名或密码错误");
                throw new BizException(401, "用户名或密码错误");
            }
            return doLogin(user, ip, device, allowedRoleKeys);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public LoginVO loginByUserId(Long userId, String ip, String device, Set<String> allowedRoleKeys) {
        TenantContext.set(0L, USER_TYPE_PLATFORM, SysRole.SCOPE_ALL, true);
        try {
            SysUser user = userId == null ? null : userMapper.selectById(userId);
            if (user == null) {
                throw new BizException(401, "用户不存在");
            }
            return doLogin(user, ip, device, allowedRoleKeys);
        } finally {
            TenantContext.clear();
        }
    }

    private LoginVO doLogin(SysUser user, String ip, String device, Set<String> allowedRoleKeys) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            recordLogin(user, ip, 0, "账号已禁用");
            throw new BizException(403, "账号已禁用");
        }
        if (user.getTenantId() != null && user.getTenantId() > 0L) {
            SysOrg tenant = orgMapper.selectById(user.getTenantId());
            if (tenant == null || tenant.getStatus() == null || tenant.getStatus() != 1) {
                recordLogin(user, ip, 0, "所属运营中心已禁用");
                throw new BizException(403, "所属运营中心已禁用");
            }
        }
        List<SysRole> roles = listRoles(user);
        if (roles.isEmpty()) {
            recordLogin(user, ip, 0, "账号未配置角色");
            throw new BizException(403, "账号未配置角色");
        }
        Set<String> roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
        if (allowedRoleKeys != null && !allowedRoleKeys.isEmpty()
                && roleKeys.stream().noneMatch(allowedRoleKeys::contains)) {
            recordLogin(user, ip, 0, "账号无权登录该端");
            throw new BizException(403, "账号无权登录该端");
        }

        String dataScope = resolveDataScope(user, roleKeys);
        List<Long> merchantIds = merchantIdsOf(user);
        Long currentMerchantId = resolveCurrentMerchant(user, merchantIds);
        StpUtil.login(user.getId(), device);
        SaSession session = StpUtil.getSession();
        session.set(SESSION_TENANT_ID, user.getTenantId() == null ? 0L : user.getTenantId());
        session.set(SESSION_USER_TYPE, user.getUserType());
        session.set(SESSION_DATA_SCOPE, dataScope);
        session.set(SESSION_MERCHANT_IDS, new ArrayList<>(merchantIds));
        if (currentMerchantId != null) {
            session.set(SESSION_MERCHANT_ID, currentMerchantId);
        }
        session.set(SESSION_ROLE_KEYS, new ArrayList<>(roleKeys));

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLogin(LocalDateTime.now());
        userMapper.updateById(update);
        recordLogin(user, ip, 1, null);

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return AuthConverter.toLoginVO(user, tokenInfo, dataScope, new ArrayList<>(roleKeys),
                permissionsByUserId(user.getId()), currentMerchantId, merchantOptions(merchantIds));
    }

    private String resolveDataScope(SysUser user, Set<String> roleKeys) {
        if (USER_TYPE_PLATFORM.equals(user.getUserType()) || roleKeys.contains(SysRole.PLATFORM_ADMIN)) {
            return SysRole.SCOPE_ALL;
        }
        if (roleKeys.contains(SysRole.TENANT_ADMIN) || roleKeys.contains(SysRole.TENANT_DUTY)) {
            return SysRole.SCOPE_TENANT;
        }
        if (roleKeys.contains(SysRole.TENANT_FIELD)) {
            return SysRole.SCOPE_WORK_AREA;
        }
        if (USER_TYPE_MERCHANT.equals(user.getUserType()) || roleKeys.contains(SysRole.MERCHANT)) {
            return SysRole.SCOPE_MERCHANT;
        }
        return SysRole.SCOPE_TENANT;
    }

    /**
     * 商户账号可管理的门店：以 sys_user_merchant 为准，旧库未迁移时回退主商户。
     */
    private List<Long> merchantIdsOf(SysUser user) {
        if (!USER_TYPE_MERCHANT.equals(user.getUserType())) {
            return Collections.emptyList();
        }
        List<Long> ids = userMerchantMapper.selectList(new LambdaQueryWrapper<SysUserMerchant>()
                        .eq(SysUserMerchant::getUserId, user.getId())
                        .orderByDesc(SysUserMerchant::getIsDefault)
                        .orderByAsc(SysUserMerchant::getMerchantId))
                .stream().map(SysUserMerchant::getMerchantId).distinct().collect(Collectors.toList());
        if (ids.isEmpty() && user.getMerchantId() != null) {
            ids.add(user.getMerchantId());
        }
        return ids;
    }

    private Long resolveCurrentMerchant(SysUser user, List<Long> merchantIds) {
        Long mainMerchantId = user.getMerchantId();
        if (mainMerchantId != null && merchantIds.contains(mainMerchantId)) {
            return mainMerchantId;
        }
        return merchantIds.isEmpty() ? mainMerchantId : merchantIds.get(0);
    }

    private Long currentSessionMerchantId(SysUser user) {
        Object value = StpUtil.getSession().get(SESSION_MERCHANT_ID);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return user.getMerchantId();
    }

    private List<Long> sessionMerchantIds(SaSession session) {
        Object value = session.get(SESSION_MERCHANT_IDS);
        if (!(value instanceof Collection)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : (Collection<?>) value) {
            if (item instanceof Number) {
                ids.add(((Number) item).longValue());
            }
        }
        return ids;
    }

    private List<MerchantOptionVO> merchantOptions(List<Long> merchantIds) {
        if (merchantIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Merchant> merchants = merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, merchant -> merchant, (a, b) -> a));
        List<MerchantOptionVO> options = new ArrayList<>(merchantIds.size());
        for (Long merchantId : merchantIds) {
            Merchant merchant = merchants.get(merchantId);
            if (merchant == null) {
                continue;
            }
            MerchantOptionVO option = new MerchantOptionVO();
            option.setId(merchant.getId());
            option.setName(merchant.getName());
            option.setAddr(merchant.getAddr());
            options.add(option);
        }
        return options;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public void switchMerchant(Long merchantId) {
        if (merchantId == null) {
            throw new BizException("请选择商户");
        }
        SaSession session = StpUtil.getSession();
        if (!sessionMerchantIds(session).contains(merchantId)) {
            throw new BizException(403, "无权切换到该商户");
        }
        session.set(SESSION_MERCHANT_ID, merchantId);
    }

    @Override
    public void changePassword(PasswordChangeDTO request) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);
        // 密码变更后使当前会话失效，前端会跳回登录页
        StpUtil.logout();
    }

    @Override
    public UserInfoVO info() {
        SysUser user = currentUser();
        List<SysRole> roles = listRoles(user);
        Set<String> roleKeys = roles.stream().map(SysRole::getRoleKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUserName(user.getUserName());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setUserType(user.getUserType());
        vo.setTenantId(user.getTenantId());
        vo.setDeptId(user.getDeptId());
        Long currentMerchantId = currentSessionMerchantId(user);
        vo.setMerchantId(currentMerchantId);
        vo.setDataScope(resolveDataScope(user, roleKeys));
        vo.setTenantName(tenantName(user.getTenantId()));
        vo.setMerchantName(merchantName(currentMerchantId));
        vo.setMerchants(merchantOptions(merchantIdsOf(user)));
        vo.setWorkAreaNames(workAreaNames(user.getId()));
        vo.setWxBound(StringUtils.hasText(user.getWxAppOpenid()));
        vo.setRoles(new ArrayList<>(roleKeys));
        vo.setPermissions(permissionsByUserId(user.getId()));
        vo.setMenus(buildMenus(user, roles));
        return vo;
    }

    @Override
    public List<MenuVO> menus() {
        SysUser user = currentUser();
        return buildMenus(user, listRoles(user));
    }

    private SysUser currentUser() {
        SysUser user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return user;
    }

    private List<SysRole> listRoles(SysUser user) {
        List<SysUserRole> relations = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).distinct()
                .collect(Collectors.toList());
        return withIgnoreTenant(() -> roleMapper.selectBatchIds(roleIds)).stream()
                .filter(role -> user.getTenantId() == null
                        ? role.getTenantId() == null || role.getTenantId() == 0L
                        : user.getTenantId().equals(role.getTenantId()))
                .collect(Collectors.toList());
    }

    private List<MenuVO> buildMenus(SysUser user, List<SysRole> roles) {
        Set<String> roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
        Long tenantId = user.getTenantId() == null ? 0L : user.getTenantId();
        if (USER_TYPE_PLATFORM.equals(user.getUserType()) || roleKeys.contains(SysRole.PLATFORM_ADMIN)) {
            return MenuConverter.toTree(menuList(0L));
        }
        if (roleKeys.contains(SysRole.TENANT_ADMIN)) {
            return MenuConverter.toTree(menuList(tenantId));
        }
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        List<SysRoleMenu> relations = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = relations.stream().map(SysRoleMenu::getMenuId).distinct()
                .collect(Collectors.toList());
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getTenantId, tenantId)
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSeq).orderByAsc(SysMenu::getId));
        return MenuConverter.toTree(menus);
    }

    private List<SysMenu> menuList(Long tenantId) {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getTenantId, tenantId)
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSeq).orderByAsc(SysMenu::getId));
    }

    @Override
    public List<String> permissionsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        SysUser user = withIgnoreTenant(() -> userMapper.selectById(userId));
        if (user == null) {
            return Collections.emptyList();
        }
        List<SysRole> roles = listRoles(user);
        Set<String> roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
        if (USER_TYPE_PLATFORM.equals(user.getUserType()) || roleKeys.contains(SysRole.PLATFORM_ADMIN)) {
            return Collections.singletonList(PERMISSION_ALL);
        }
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        List<SysRoleMenu> relations = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = relations.stream().map(SysRoleMenu::getMenuId).distinct()
                .collect(Collectors.toList());
        Long tenantId = user.getTenantId() == null ? 0L : user.getTenantId();
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getId, menuIds)
                        .eq(SysMenu::getTenantId, tenantId)
                        .eq(SysMenu::getStatus, 1)
                        .isNotNull(SysMenu::getPerms)
                        .ne(SysMenu::getPerms, ""))
                .stream().map(SysMenu::getPerms).distinct().collect(Collectors.toList());
    }

    private String tenantName(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return PLATFORM_NAME;
        }
        SysOrg org = orgMapper.selectById(tenantId);
        return org == null ? null : org.getName();
    }

    private String merchantName(Long merchantId) {
        if (merchantId == null) {
            return null;
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        return merchant == null ? null : merchant.getName();
    }

    private List<String> workAreaNames(Long userId) {
        List<Long> workAreaIds = userWorkAreaMapper.selectList(
                        new LambdaQueryWrapper<SysUserWorkArea>().eq(SysUserWorkArea::getUserId, userId))
                .stream().map(SysUserWorkArea::getWorkAreaId).distinct().collect(Collectors.toList());
        if (workAreaIds.isEmpty()) {
            return Collections.emptyList();
        }
        return workAreaMapper.selectBatchIds(workAreaIds).stream()
                .map(SysWorkArea::getName).collect(Collectors.toList());
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

    private void recordLogin(SysUser user, String ip, int result, String reason) {
        LogLogin log = new LogLogin();
        log.setTenantId(user == null ? null : user.getTenantId());
        log.setUserId(user == null ? null : user.getId());
        log.setUserType(user == null ? null : user.getUserType());
        log.setLoginTime(LocalDateTime.now());
        log.setLoginIp(ip);
        log.setLoginResult(result);
        log.setFailureReason(reason);
        logLoginMapper.insert(log);
    }
}
