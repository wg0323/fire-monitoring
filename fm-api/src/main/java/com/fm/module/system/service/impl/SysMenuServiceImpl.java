package com.fm.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.system.baseline.MenuBaseline;
import com.fm.module.system.dto.MenuSaveDTO;
import com.fm.module.system.entity.SysMenu;
import com.fm.module.system.entity.SysRoleMenu;
import com.fm.module.system.mapper.SysMenuMapper;
import com.fm.module.system.mapper.SysRoleMenuMapper;
import com.fm.module.system.service.SysMenuService;
import com.fm.module.system.support.AdminGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 菜单维护：平台只改 tenant 0，租户管理员只改本租户。
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private static final long ROOT_ID = 0L;
    private static final long PLATFORM_TENANT_ID = 0L;

    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<SysMenu> listTreeNodes() {
        AdminGuard.requireAdmin();
        return baseMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getTenantId, currentTenantId())
                .orderByAsc(SysMenu::getSeq)
                .orderByAsc(SysMenu::getId));
    }

    @Override
    @Transactional
    public Long create(MenuSaveDTO request) {
        AdminGuard.requireAdmin();
        validate(request);
        Long tenantId = currentTenantId();
        checkMenuKeyUnique(tenantId, request.getMenuKey(), null);
        Long parentId = request.getParentId() == null ? ROOT_ID : request.getParentId();
        validateParent(tenantId, parentId, null);

        SysMenu menu = new SysMenu();
        menu.setTenantId(tenantId);
        menu.setParentId(parentId);
        menu.setName(request.getName());
        menu.setMenuKey(request.getMenuKey());
        menu.setType(request.getType());
        menu.setPath(request.getPath());
        menu.setPerms(request.getPerms());
        menu.setSeq(request.getSeq() == null ? 0 : request.getSeq());
        menu.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        menu.setPlatformOnly(AdminGuard.isPlatform()
                ? request.getPlatformOnly() : MenuBaseline.TENANT_VISIBLE);
        baseMapper.insert(menu);
        return menu.getId();
    }

    @Override
    @Transactional
    public void update(Long id, MenuSaveDTO request) {
        AdminGuard.requireAdmin();
        SysMenu existing = requireAccessible(id);
        validate(request);
        Long tenantId = existing.getTenantId();
        checkMenuKeyUnique(tenantId, request.getMenuKey(), id);
        Long parentId = request.getParentId() == null ? ROOT_ID : request.getParentId();
        validateParent(tenantId, parentId, id);

        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setName(request.getName());
        menu.setMenuKey(request.getMenuKey());
        menu.setType(request.getType());
        menu.setPath(request.getPath());
        menu.setPerms(request.getPerms());
        menu.setSeq(request.getSeq() == null ? 0 : request.getSeq());
        menu.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        menu.setPlatformOnly(AdminGuard.isPlatform()
                ? request.getPlatformOnly() : MenuBaseline.TENANT_VISIBLE);
        baseMapper.updateById(menu);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AdminGuard.requireAdmin();
        requireAccessible(id);
        Long children = baseMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id)
                .eq(SysMenu::getTenantId, currentTenantId()));
        if (children != null && children > 0) {
            throw new BizException("存在子菜单，不能删除");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
        // 菜单是配置数据，物理删除，避免软删行占用 (tenant_id, menu_key) 唯一键
        baseMapper.physicalDeleteById(id);
    }

    private SysMenu requireAccessible(Long id) {
        SysMenu menu = baseMapper.selectById(id);
        if (menu == null) {
            throw new BizException("菜单不存在或无权操作");
        }
        if (AdminGuard.isPlatform()) {
            if (!menu.getTenantId().equals(PLATFORM_TENANT_ID)) {
                throw new BizException(403, "平台管理员只能维护平台基线菜单");
            }
        } else if (!menu.getTenantId().equals(currentTenantId())) {
            throw new BizException(403, "不能维护其他租户的菜单");
        }
        return menu;
    }

    private void validate(MenuSaveDTO request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("菜单名称不能为空");
        }
        if (!StringUtils.hasText(request.getMenuKey())) {
            throw new BizException("菜单标识不能为空");
        }
        Integer type = request.getType();
        if (type == null || (type != MenuBaseline.TYPE_DIR && type != MenuBaseline.TYPE_MENU
                && type != MenuBaseline.TYPE_BUTTON)) {
            throw new BizException("菜单类型不合法");
        }
        if (type == MenuBaseline.TYPE_MENU && !StringUtils.hasText(request.getPath())) {
            throw new BizException("菜单路由不能为空");
        }
        if (type == MenuBaseline.TYPE_BUTTON && !StringUtils.hasText(request.getPerms())) {
            throw new BizException("按钮权限标识不能为空");
        }
    }

    private void checkMenuKeyUnique(Long tenantId, String menuKey, Long excludeId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getTenantId, tenantId)
                .eq(SysMenu::getMenuKey, menuKey);
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        Long count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("菜单标识已存在");
        }
    }

    private void validateParent(Long tenantId, Long parentId, Long selfId) {
        if (parentId == null || parentId == ROOT_ID) {
            return;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new BizException("上级菜单不能是自己");
        }
        SysMenu parent = baseMapper.selectById(parentId);
        if (parent == null || !tenantId.equals(parent.getTenantId())) {
            throw new BizException("上级菜单不存在或不属于当前租户");
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != ROOT_ID && visited.add(cursor)) {
            if (selfId != null && cursor.equals(selfId)) {
                throw new BizException("上级菜单不能形成环");
            }
            SysMenu node = baseMapper.selectById(cursor);
            if (node == null) {
                break;
            }
            cursor = node.getParentId();
        }
    }

    private Long currentTenantId() {
        return AdminGuard.isPlatform() ? PLATFORM_TENANT_ID : TenantContext.getTenantId();
    }
}
