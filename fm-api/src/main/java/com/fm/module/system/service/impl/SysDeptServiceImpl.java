package com.fm.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.system.converter.DeptConverter;
import com.fm.module.system.dto.DeptSaveDTO;
import com.fm.module.system.entity.SysDept;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.mapper.SysDeptMapper;
import com.fm.module.system.mapper.SysOrgMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.service.SysDeptService;
import com.fm.module.system.support.AdminGuard;
import com.fm.module.system.vo.DeptVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;

/**
 * 部门维护：平台跨租户，租户管理员限本租户。
 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    private static final long ROOT_ID = 0L;

    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;

    @Override
    public List<DeptVO> tree(Long tenantId) {
        AdminGuard.requireAdmin();
        Long targetTenant = resolveTargetTenant(tenantId);
        List<SysDept> depts = scoped(() -> baseMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getTenantId, targetTenant)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getId)));
        return DeptConverter.toTree(depts);
    }

    @Override
    @Transactional
    public Long create(DeptSaveDTO request) {
        AdminGuard.requireAdmin();
        validate(request);
        Long tenantId = resolveTargetTenant(request.getTenantId());
        Long parentId = request.getParentId() == null ? ROOT_ID : request.getParentId();
        validateParent(tenantId, parentId);
        SysDept dept = new SysDept();
        dept.setTenantId(tenantId);
        dept.setParentId(parentId);
        dept.setName(request.getName());
        dept.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        scoped(() -> baseMapper.insert(dept));
        return dept.getId();
    }

    @Override
    @Transactional
    public void update(Long id, DeptSaveDTO request) {
        AdminGuard.requireAdmin();
        requireAccessible(id);
        validate(request);
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setName(request.getName());
        dept.setStatus(request.getStatus());
        scoped(() -> baseMapper.updateById(dept));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AdminGuard.requireAdmin();
        SysDept dept = requireAccessible(id);
        Long children = scoped(() -> baseMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id)
                .eq(SysDept::getTenantId, dept.getTenantId())));
        if (children != null && children > 0) {
            throw new BizException("存在子部门，不能删除");
        }
        Long users = scoped(() -> userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, id)
                .eq(SysUser::getTenantId, dept.getTenantId())));
        if (users != null && users > 0) {
            throw new BizException("部门下还有用户，不能删除");
        }
        scoped(() -> {
            baseMapper.deleteById(id);
            return null;
        });
    }

    private SysDept requireAccessible(Long id) {
        SysDept dept = scoped(() -> baseMapper.selectById(id));
        if (dept == null) {
            throw new BizException("部门不存在或无权操作");
        }
        if (!AdminGuard.isPlatform() && !dept.getTenantId().equals(TenantContext.getTenantId())) {
            throw new BizException(403, "不能维护其他租户的部门");
        }
        return dept;
    }

    private void validate(DeptSaveDTO request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("部门名称不能为空");
        }
    }

    private void validateParent(Long tenantId, Long parentId) {
        if (parentId == null || parentId == ROOT_ID) {
            return;
        }
        SysDept parent = scoped(() -> baseMapper.selectById(parentId));
        if (parent == null || !tenantId.equals(parent.getTenantId())) {
            throw new BizException("上级部门不存在或不属于当前租户");
        }
    }

    private Long resolveTargetTenant(Long requested) {
        if (AdminGuard.isPlatform()) {
            if (requested == null) {
                throw new BizException("请选择所属运营中心");
            }
            SysOrg org = orgMapper.selectById(requested);
            if (org == null || !SysOrg.TYPE_TENANT.equals(org.getType()) || org.getStatus() == null
                    || org.getStatus() != 1) {
                throw new BizException("所属运营中心不存在或已禁用");
            }
            return requested;
        }
        return TenantContext.getTenantId();
    }

    private <T> T scoped(Supplier<T> action) {
        if (!AdminGuard.isPlatform()) {
            return action.get();
        }
        boolean previous = TenantContext.isIgnoreTenant();
        TenantContext.setIgnoreTenant(true);
        try {
            return action.get();
        } finally {
            TenantContext.setIgnoreTenant(previous);
        }
    }
}
