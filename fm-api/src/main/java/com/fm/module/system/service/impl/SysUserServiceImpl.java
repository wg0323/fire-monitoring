package com.fm.module.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.system.converter.UserConverter;
import com.fm.module.system.dto.UserQueryDTO;
import com.fm.module.system.dto.UserSaveDTO;
import com.fm.module.system.entity.SysDept;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysUserRole;
import com.fm.module.system.entity.SysUserWorkArea;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.mapper.SysDeptMapper;
import com.fm.module.system.mapper.SysOrgMapper;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.service.SysUserService;
import com.fm.module.system.support.AdminGuard;
import com.fm.module.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户维护：手机号全局唯一，角色/商户/工作区必须与目标租户一致。
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final long PLATFORM_TENANT_ID = 0L;
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String USER_TYPE_PLATFORM = "PLATFORM";
    private static final String USER_TYPE_TENANT = "TENANT";
    private static final String USER_TYPE_MERCHANT = "MERCHANT";
    private static final String PLATFORM_NAME = "平台";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Set<String> TENANT_ROLE_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    SysRole.TENANT_ADMIN, SysRole.TENANT_DUTY, SysRole.TENANT_FIELD)));
    private static final Set<String> MERCHANT_ROLE_KEYS = Collections.singleton(SysRole.MERCHANT);
    private static final Set<String> PLATFORM_ROLE_KEYS = Collections.singleton(SysRole.PLATFORM_ADMIN);

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserWorkAreaMapper userWorkAreaMapper;
    private final SysWorkAreaMapper workAreaMapper;
    private final SysDeptMapper deptMapper;
    private final SysOrgMapper orgMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserVO> page(UserQueryDTO query) {
        AdminGuard.requireAdmin();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(SysUser::getUserName, query.getKeyword())
                    .or().like(SysUser::getRealName, query.getKeyword())
                    .or().like(SysUser::getPhone, query.getKeyword()));
        }
        wrapper.eq(StringUtils.hasText(query.getUserType()), SysUser::getUserType, query.getUserType());
        wrapper.eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());
        Long tenantId = AdminGuard.isPlatform() ? query.getTenantId() : TenantContext.getTenantId();
        wrapper.eq(tenantId != null, SysUser::getTenantId, tenantId);
        wrapper.orderByDesc(SysUser::getCreateTime).orderByDesc(SysUser::getId);

        Page<SysUser> entityPage = scoped(() -> baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper));
        Page<UserVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<UserVO> records = entityPage.getRecords().stream()
                .map(UserConverter::toVO).collect(Collectors.toList());
        fillUserVOs(records);
        result.setRecords(records);
        return result;
    }

    @Override
    public UserVO detail(Long id) {
        AdminGuard.requireAdmin();
        SysUser user = requireAccessible(id);
        List<UserVO> records = new ArrayList<>();
        records.add(UserConverter.toVO(user));
        fillUserVOs(records);
        return records.get(0);
    }

    @Override
    @Transactional
    public Long create(UserSaveDTO request) {
        AdminGuard.requireAdmin();
        String userType = normalizeUserType(request.getUserType());
        Long tenantId = resolveTargetTenant(request.getTenantId(), userType);
        SysRole role = resolveRole(request.getRoleId(), tenantId, userType);
        validatePhone(request.getPhone(), null);
        validateUserName(request.getUserName(), null);
        validateDept(request.getDeptId(), tenantId);
        Long merchantId = validateMerchant(request.getMerchantId(), tenantId, userType);
        List<Long> workAreaIds = normalizeWorkAreas(request.getWorkAreaIds(), tenantId,
                role.getRoleKey());

        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setDeptId(request.getDeptId());
        user.setUserType(userType);
        user.setUserName(request.getUserName());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setMerchantId(merchantId);
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        String password = StringUtils.hasText(request.getPassword())
                ? request.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(password));
        scoped(() -> baseMapper.insert(user));
        replaceUserRole(user.getId(), role.getId());
        replaceWorkAreas(user.getId(), tenantId, workAreaIds);
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, UserSaveDTO request) {
        AdminGuard.requireAdmin();
        SysUser existing = requireAccessible(id);
        if (StringUtils.hasText(request.getUserType())
                && !existing.getUserType().equals(request.getUserType())) {
            throw new BizException("用户类型不允许修改");
        }
        if (StringUtils.hasText(request.getUserName())
                && !existing.getUserName().equals(request.getUserName())) {
            throw new BizException("登录名不允许修改");
        }
        SysRole currentRole = currentRole(existing.getId());
        SysRole targetRole = request.getRoleId() == null ? currentRole
                : resolveRole(request.getRoleId(), existing.getTenantId(), existing.getUserType());
        if (targetRole == null) {
            throw new BizException("请选择角色");
        }
        validatePhone(request.getPhone(), existing.getId());
        validateDept(request.getDeptId(), existing.getTenantId());
        Long merchantId = validateMerchant(request.getMerchantId(), existing.getTenantId(),
                existing.getUserType());
        List<Long> workAreaIds = request.getWorkAreaIds() == null
                ? currentWorkAreaIds(existing.getId())
                : normalizeWorkAreas(request.getWorkAreaIds(), existing.getTenantId(),
                targetRole.getRoleKey());
        if (SysRole.TENANT_FIELD.equals(targetRole.getRoleKey()) && workAreaIds.isEmpty()) {
            throw new BizException("外勤人员至少绑定一个工作区");
        }

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getRealName, request.getRealName())
                .set(SysUser::getPhone, request.getPhone())
                .set(SysUser::getDeptId, request.getDeptId())
                .set(SysUser::getMerchantId, merchantId)
                .set(SysUser::getStatus, request.getStatus() == null ? existing.getStatus() : request.getStatus())
                .set(SysUser::getUpdateTime, LocalDateTime.now());
        scoped(() -> baseMapper.update(null, wrapper));
        if (request.getRoleId() != null) {
            replaceUserRole(id, targetRole.getId());
        }
        if (request.getWorkAreaIds() != null) {
            replaceWorkAreas(id, existing.getTenantId(), workAreaIds);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AdminGuard.requireAdmin();
        if (id != null && id.equals(StpUtil.getLoginIdAsLong())) {
            throw new BizException("不能删除当前登录账号");
        }
        requireAccessible(id);
        scoped(() -> baseMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getPhone, null)
                .set(SysUser::getUpdateTime, LocalDateTime.now())));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        scopedVoid(() -> userWorkAreaMapper.delete(
                new LambdaQueryWrapper<SysUserWorkArea>().eq(SysUserWorkArea::getUserId, id)));
        scopedVoid(() -> baseMapper.deleteById(id));
    }

    @Override
    public void resetPassword(Long id, String password) {
        AdminGuard.requireAdmin();
        requireAccessible(id);
        String target = StringUtils.hasText(password) ? password : DEFAULT_PASSWORD;
        scoped(() -> baseMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword, passwordEncoder.encode(target))
                .set(SysUser::getUpdateTime, LocalDateTime.now())));
    }

    @Override
    @Transactional
    public void bindWorkAreas(Long id, List<Long> workAreaIds) {
        AdminGuard.requireAdmin();
        SysUser user = requireAccessible(id);
        SysRole role = currentRole(id);
        List<Long> normalized = normalizeWorkAreas(workAreaIds, user.getTenantId(),
                role == null ? null : role.getRoleKey());
        replaceWorkAreas(id, user.getTenantId(), normalized);
    }

    @Override
    public List<UserVO> options(Long tenantId, String userType) {
        AdminGuard.requireAdmin();
        Long targetTenant = AdminGuard.isPlatform() ? tenantId : TenantContext.getTenantId();
        if (targetTenant == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, targetTenant)
                .eq(SysUser::getStatus, 1)
                .eq(StringUtils.hasText(userType), SysUser::getUserType, userType)
                .orderByAsc(SysUser::getId)
                .last("LIMIT 200");
        List<SysUser> users = scoped(() -> baseMapper.selectList(wrapper));
        return users.stream().map(UserConverter::toVO).collect(Collectors.toList());
    }

    private SysUser requireAccessible(Long id) {
        SysUser user = scoped(() -> baseMapper.selectById(id));
        if (user == null) {
            throw new BizException("用户不存在或无权操作");
        }
        if (!AdminGuard.isPlatform() && !user.getTenantId().equals(TenantContext.getTenantId())) {
            throw new BizException(403, "不能维护其他租户的用户");
        }
        return user;
    }

    private String normalizeUserType(String userType) {
        if (!StringUtils.hasText(userType)) {
            throw new BizException("请选择用户类型");
        }
        if (USER_TYPE_PLATFORM.equals(userType)) {
            if (!AdminGuard.isPlatform()) {
                throw new BizException(403, "仅平台管理员可创建平台账号");
            }
            return USER_TYPE_PLATFORM;
        }
        if (USER_TYPE_MERCHANT.equals(userType)) {
            if (AdminGuard.isPlatform()) {
                throw new BizException(403, "商户账号由租户管理员创建");
            }
            return USER_TYPE_MERCHANT;
        }
        if (USER_TYPE_TENANT.equals(userType)) {
            return USER_TYPE_TENANT;
        }
        throw new BizException("用户类型不合法");
    }

    private Long resolveTargetTenant(Long requestedTenantId, String userType) {
        if (USER_TYPE_PLATFORM.equals(userType)) {
            return PLATFORM_TENANT_ID;
        }
        Long tenantId;
        if (AdminGuard.isPlatform()) {
            if (requestedTenantId == null) {
                throw new BizException("请选择所属运营中心");
            }
            tenantId = requestedTenantId;
        } else {
            if (requestedTenantId != null && !requestedTenantId.equals(TenantContext.getTenantId())) {
                throw new BizException(403, "不能创建其他租户的账号");
            }
            tenantId = TenantContext.getTenantId();
        }
        SysOrg org = orgMapper.selectById(tenantId);
        if (org == null || !SysOrg.TYPE_TENANT.equals(org.getType()) || org.getStatus() == null
                || org.getStatus() != 1) {
            throw new BizException("所属运营中心不存在或已禁用");
        }
        return tenantId;
    }

    private SysRole resolveRole(Long roleId, Long tenantId, String userType) {
        if (roleId == null) {
            throw new BizException("请选择角色");
        }
        SysRole role = withIgnoreTenant(() -> roleMapper.selectById(roleId));
        if (role == null || role.getStatus() == null || role.getStatus() != 1) {
            throw new BizException("角色不存在或已禁用");
        }
        if (!tenantId.equals(role.getTenantId())) {
            throw new BizException(403, "角色不属于目标运营中心");
        }
        Set<String> allowed = allowedRoleKeys(userType);
        if (!allowed.contains(role.getRoleKey())) {
            throw new BizException("角色与用户类型不匹配");
        }
        return role;
    }

    private Set<String> allowedRoleKeys(String userType) {
        if (USER_TYPE_PLATFORM.equals(userType)) {
            return PLATFORM_ROLE_KEYS;
        }
        if (USER_TYPE_MERCHANT.equals(userType)) {
            return MERCHANT_ROLE_KEYS;
        }
        return TENANT_ROLE_KEYS;
    }

    private void validateUserName(String userName, Long excludeId) {
        if (!StringUtils.hasText(userName)) {
            throw new BizException("登录名不能为空");
        }
        SysUser exists = baseMapper.selectByUserName(userName);
        if (exists != null && (excludeId == null || !excludeId.equals(exists.getId()))) {
            throw new BizException("登录名已存在");
        }
    }

    private void validatePhone(String phone, Long excludeId) {
        if (!StringUtils.hasText(phone)) {
            throw new BizException("手机号不能为空");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BizException("手机号格式不正确");
        }
        List<SysUser> users = baseMapper.selectByPhone(phone);
        boolean duplicated = users.stream()
                .anyMatch(user -> excludeId == null || !excludeId.equals(user.getId()));
        if (duplicated) {
            throw new BizException("手机号已存在");
        }
    }

    private void validateDept(Long deptId, Long tenantId) {
        if (deptId == null) {
            return;
        }
        SysDept dept = withIgnoreTenant(() -> deptMapper.selectById(deptId));
        if (dept == null || !tenantId.equals(dept.getTenantId())) {
            throw new BizException("部门不属于目标运营中心");
        }
    }

    private Long validateMerchant(Long merchantId, Long tenantId, String userType) {
        if (!USER_TYPE_MERCHANT.equals(userType)) {
            if (merchantId != null) {
                throw new BizException("仅商户账号可绑定商户");
            }
            return null;
        }
        if (merchantId == null) {
            throw new BizException("商户账号必须绑定商户");
        }
        Merchant merchant = withIgnoreTenant(() -> merchantMapper.selectById(merchantId));
        if (merchant == null || !tenantId.equals(merchant.getTenantId())) {
            throw new BizException("商户不属于目标运营中心");
        }
        return merchantId;
    }

    private List<Long> normalizeWorkAreas(List<Long> workAreaIds, Long tenantId, String roleKey) {
        List<Long> targetIds = workAreaIds == null ? Collections.emptyList()
                : workAreaIds.stream().distinct().collect(Collectors.toList());
        if (SysRole.TENANT_FIELD.equals(roleKey) && targetIds.isEmpty()) {
            throw new BizException("外勤人员至少绑定一个工作区");
        }
        if (targetIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysWorkArea> areas = withIgnoreTenant(() -> workAreaMapper.selectBatchIds(targetIds));
        if (areas.size() != targetIds.size()
                || areas.stream().anyMatch(area -> !tenantId.equals(area.getTenantId()))) {
            throw new BizException("工作区不属于目标运营中心");
        }
        return targetIds;
    }

    private SysRole currentRole(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (relations.isEmpty()) {
            return null;
        }
        return scoped(() -> roleMapper.selectById(relations.get(0).getRoleId()));
    }

    private List<Long> currentWorkAreaIds(Long userId) {
        return scoped(() -> userWorkAreaMapper.selectList(new LambdaQueryWrapper<SysUserWorkArea>()
                        .eq(SysUserWorkArea::getUserId, userId)))
                .stream().map(SysUserWorkArea::getWorkAreaId).collect(Collectors.toList());
    }

    private void replaceUserRole(Long userId, Long roleId) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        userRoleMapper.insert(relation);
    }

    private void replaceWorkAreas(Long userId, Long tenantId, List<Long> workAreaIds) {
        scopedVoid(() -> userWorkAreaMapper.delete(new LambdaQueryWrapper<SysUserWorkArea>()
                .eq(SysUserWorkArea::getUserId, userId)));
        for (Long workAreaId : workAreaIds) {
            SysUserWorkArea relation = new SysUserWorkArea();
            relation.setTenantId(tenantId);
            relation.setUserId(userId);
            relation.setWorkAreaId(workAreaId);
            scopedVoid(() -> userWorkAreaMapper.insert(relation));
        }
    }

    private void fillUserVOs(List<UserVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(UserVO::getId).collect(Collectors.toList());
        Map<Long, SysRole> roles = loadRoles(userIds);
        Map<Long, List<Long>> workAreaIds = loadWorkAreaIds(userIds);
        Map<Long, SysWorkArea> workAreas = loadWorkAreas(workAreaIds);
        Map<Long, SysDept> depts = loadDepts(records);
        Map<Long, Merchant> merchants = loadMerchants(records);
        Map<Long, String> tenantNames = loadTenantNames(records);

        for (UserVO record : records) {
            SysRole role = roles.get(record.getId());
            if (role != null) {
                record.setRoleId(role.getId());
                record.setRoleKey(role.getRoleKey());
                record.setRoleName(role.getRoleName());
            }
            List<Long> ids = workAreaIds.getOrDefault(record.getId(), Collections.emptyList());
            record.setWorkAreaIds(ids);
            record.setWorkAreaNames(ids.stream()
                    .map(workAreas::get).filter(area -> area != null)
                    .map(SysWorkArea::getName).collect(Collectors.toList()));
            if (record.getDeptId() != null && depts.containsKey(record.getDeptId())) {
                record.setDeptName(depts.get(record.getDeptId()).getName());
            }
            if (record.getMerchantId() != null && merchants.containsKey(record.getMerchantId())) {
                record.setMerchantName(merchants.get(record.getMerchantId()).getName());
            }
            Long tenantId = record.getTenantId();
            record.setTenantName(tenantId == null || tenantId == PLATFORM_TENANT_ID
                    ? PLATFORM_NAME : tenantNames.get(tenantId));
        }
    }

    private Map<Long, SysRole> loadRoles(List<Long> userIds) {
        List<SysUserRole> relations = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, userIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).distinct()
                .collect(Collectors.toList());
        Map<Long, SysRole> roleMap = scoped(() -> roleMapper.selectBatchIds(roleIds)).stream()
                .collect(Collectors.toMap(SysRole::getId, role -> role, (a, b) -> a));
        Map<Long, SysRole> result = new HashMap<>();
        for (SysUserRole relation : relations) {
            SysRole role = roleMap.get(relation.getRoleId());
            if (role != null) {
                result.putIfAbsent(relation.getUserId(), role);
            }
        }
        return result;
    }

    private Map<Long, List<Long>> loadWorkAreaIds(List<Long> userIds) {
        List<SysUserWorkArea> relations = scoped(() -> userWorkAreaMapper.selectList(
                new LambdaQueryWrapper<SysUserWorkArea>().in(SysUserWorkArea::getUserId, userIds)));
        Map<Long, List<Long>> result = new HashMap<>();
        for (SysUserWorkArea relation : relations) {
            result.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>())
                    .add(relation.getWorkAreaId());
        }
        return result;
    }

    private Map<Long, SysWorkArea> loadWorkAreas(Map<Long, List<Long>> workAreaIds) {
        List<Long> ids = workAreaIds.values().stream().flatMap(List::stream).distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return scoped(() -> workAreaMapper.selectBatchIds(ids)).stream()
                .collect(Collectors.toMap(SysWorkArea::getId, area -> area, (a, b) -> a));
    }

    private Map<Long, SysDept> loadDepts(List<UserVO> records) {
        List<Long> ids = records.stream().map(UserVO::getDeptId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return scoped(() -> deptMapper.selectBatchIds(ids)).stream()
                .collect(Collectors.toMap(SysDept::getId, dept -> dept, (a, b) -> a));
    }

    private Map<Long, Merchant> loadMerchants(List<UserVO> records) {
        List<Long> ids = records.stream().map(UserVO::getMerchantId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return scoped(() -> merchantMapper.selectBatchIds(ids)).stream()
                .collect(Collectors.toMap(Merchant::getId, merchant -> merchant, (a, b) -> a));
    }

    private Map<Long, String> loadTenantNames(List<UserVO> records) {
        List<Long> ids = records.stream().map(UserVO::getTenantId)
                .filter(id -> id != null && id != PLATFORM_TENANT_ID).distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return orgMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysOrg::getId, SysOrg::getName, (a, b) -> a));
    }

    private <T> T scoped(Supplier<T> action) {
        if (!AdminGuard.isPlatform()) {
            return action.get();
        }
        return withIgnoreTenant(action);
    }

    private void scopedVoid(Runnable action) {
        if (!AdminGuard.isPlatform()) {
            action.run();
            return;
        }
        withIgnoreTenant(() -> {
            action.run();
            return null;
        });
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
