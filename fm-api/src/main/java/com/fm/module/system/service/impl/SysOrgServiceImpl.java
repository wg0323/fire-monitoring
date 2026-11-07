package com.fm.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictBaselineService;
import com.fm.module.system.converter.OrgConverter;
import com.fm.module.system.dto.OrgQueryDTO;
import com.fm.module.system.dto.OrgSaveDTO;
import com.fm.module.system.entity.SysMenu;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysRoleMenu;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysUserRole;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.mapper.SysMenuMapper;
import com.fm.module.system.mapper.SysOrgMapper;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysRoleMenuMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.service.MenuBaselineService;
import com.fm.module.system.service.RoleBaselineService;
import com.fm.module.system.service.SysOrgService;
import com.fm.module.system.support.AdminGuard;
import com.fm.module.system.vo.OrgVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 运营中心维护：新建时复制一次菜单基线，复制后与基线脱钩。
 */
@Service
@RequiredArgsConstructor
public class SysOrgServiceImpl extends ServiceImpl<SysOrgMapper, SysOrg> implements SysOrgService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysWorkAreaMapper workAreaMapper;
    private final SysMenuMapper menuMapper;
    private final MerchantMapper merchantMapper;
    private final MenuBaselineService menuBaselineService;
    private final RoleBaselineService roleBaselineService;
    private final DictBaselineService dictBaselineService;

    @Override
    public Page<OrgVO> page(OrgQueryDTO query) {
        AdminGuard.requirePlatform();
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getType, SysOrg.TYPE_TENANT)
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysOrg::getName, query.getKeyword())
                        .or().like(SysOrg::getCode, query.getKeyword()))
                .eq(query.getStatus() != null, SysOrg::getStatus, query.getStatus())
                .orderByAsc(SysOrg::getId);
        Page<SysOrg> entityPage = baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        Page<OrgVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream()
                .map(OrgConverter::toVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<OrgVO> options() {
        AdminGuard.requirePlatform();
        return baseMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                        .eq(SysOrg::getType, SysOrg.TYPE_TENANT)
                        .eq(SysOrg::getStatus, 1)
                        .orderByAsc(SysOrg::getId))
                .stream().map(OrgConverter::toVO).collect(Collectors.toList());
    }

    @Override
    public OrgVO detail(Long id) {
        AdminGuard.requirePlatform();
        return OrgConverter.toVO(requireTenant(id));
    }

    @Override
    @Transactional
    public Long create(OrgSaveDTO request) {
        AdminGuard.requirePlatform();
        validate(request);
        SysOrg org = new SysOrg();
        org.setType(SysOrg.TYPE_TENANT);
        org.setParentId(null);
        org.setName(request.getName());
        org.setCode(request.getCode());
        org.setAdcode(request.getAdcode());
        org.setAddress(request.getAddress());
        org.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        baseMapper.insert(org);
        int copied = menuBaselineService.copyBaseline(org.getId());
        if (copied == 0) {
            throw new BizException("菜单基线为空，运营中心创建失败");
        }
        int roles = roleBaselineService.provisionTenantRoles(org.getId());
        if (roles != 4) {
            throw new BizException("租户角色基线创建失败");
        }
        dictBaselineService.provisionTenantDict(org.getId());
        return org.getId();
    }

    @Override
    @Transactional
    public void update(Long id, OrgSaveDTO request) {
        AdminGuard.requirePlatform();
        requireTenant(id);
        validate(request);
        SysOrg org = new SysOrg();
        org.setId(id);
        org.setName(request.getName());
        org.setCode(request.getCode());
        org.setAdcode(request.getAdcode());
        org.setAddress(request.getAddress());
        org.setStatus(request.getStatus());
        baseMapper.updateById(org);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AdminGuard.requirePlatform();
        requireTenant(id);
        long users = countIgnoringTenant(() -> userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, id)));
        long areas = countIgnoringTenant(() -> workAreaMapper.selectCount(new LambdaQueryWrapper<SysWorkArea>()
                .eq(SysWorkArea::getTenantId, id)));
        long merchants = countIgnoringTenant(() -> merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getTenantId, id)));
        if (users > 0 || areas > 0 || merchants > 0) {
            throw new BizException("运营中心下还有用户、工作区或商户，不能删除");
        }
        withIgnoreTenant(() -> {
            List<SysRole> tenantRoles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getTenantId, id));
            List<Long> roleIds = tenantRoles.stream().map(SysRole::getId).collect(Collectors.toList());
            if (!roleIds.isEmpty()) {
                roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, roleIds));
                userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                        .in(SysUserRole::getRoleId, roleIds));
                roleMapper.delete(new LambdaQueryWrapper<SysRole>().eq(SysRole::getTenantId, id));
            }
            menuMapper.delete(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getTenantId, id));
            dictBaselineService.removeTenantDict(id);
            return null;
        });
        baseMapper.deleteById(id);
    }

    private SysOrg requireTenant(Long id) {
        SysOrg org = id == null ? null : baseMapper.selectById(id);
        if (org == null || !SysOrg.TYPE_TENANT.equals(org.getType())) {
            throw new BizException("运营中心不存在");
        }
        return org;
    }

    private void validate(OrgSaveDTO request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("运营中心名称不能为空");
        }
    }

    private long countIgnoringTenant(Supplier<Long> supplier) {
        Long count = withIgnoreTenant(supplier);
        return count == null ? 0L : count;
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
