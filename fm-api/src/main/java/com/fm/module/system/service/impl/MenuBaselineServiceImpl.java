package com.fm.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.system.baseline.MenuBaseline;
import com.fm.module.system.entity.SysMenu;
import com.fm.module.system.mapper.SysMenuMapper;
import com.fm.module.system.service.MenuBaselineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 菜单基线：tenant 0 维护，新租户复制后脱钩。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuBaselineServiceImpl implements MenuBaselineService {

    private static final long PLATFORM_TENANT_ID = 0L;

    private final SysMenuMapper menuMapper;

    @Override
    @Transactional
    public int ensureBaseline() {
        return withIgnoreTenant(() -> {
            int inserted = 0;
            for (MenuBaseline.MenuSeed seed : MenuBaseline.list()) {
                Long exists = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getTenantId, PLATFORM_TENANT_ID)
                        .eq(SysMenu::getMenuKey, seed.getMenuKey()));
                if (exists != null && exists > 0) {
                    continue;
                }
                SysMenu menu = new SysMenu();
                menu.setId(seed.getId());
                menu.setTenantId(PLATFORM_TENANT_ID);
                menu.setParentId(seed.getParentId());
                menu.setName(seed.getName());
                menu.setMenuKey(seed.getMenuKey());
                menu.setType(seed.getType());
                menu.setPath(seed.getPath());
                menu.setPerms(seed.getPerms());
                menu.setSeq(seed.getSeq());
                menu.setStatus(1);
                menu.setPlatformOnly(seed.getPlatformOnly());
                menuMapper.insert(menu);
                inserted++;
            }
            return inserted;
        });
    }

    @Override
    @Transactional
    public int copyBaseline(Long tenantId) {
        if (tenantId == null || tenantId == PLATFORM_TENANT_ID) {
            throw new BizException("租户 id 不合法");
        }
        return withIgnoreTenant(() -> {
            Long exists = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getTenantId, tenantId));
            if (exists != null && exists > 0) {
                log.info("tenant {} already has menus, skip baseline copy", tenantId);
                return 0;
            }
            List<SysMenu> baseline = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getTenantId, PLATFORM_TENANT_ID)
                    .orderByAsc(SysMenu::getSeq).orderByAsc(SysMenu::getId));
            if (baseline.isEmpty()) {
                log.warn("menu baseline is empty, skip copy for tenant {}", tenantId);
                return 0;
            }
            Map<Long, Long> idMap = new HashMap<>(baseline.size());
            for (SysMenu menu : baseline) {
                idMap.put(menu.getId(), IdWorker.getId());
            }
            int copied = 0;
            for (SysMenu menu : baseline) {
                if (menu.getPlatformOnly() != null && menu.getPlatformOnly() == MenuBaseline.PLATFORM_ONLY) {
                    continue;
                }
                SysMenu copy = new SysMenu();
                copy.setId(idMap.get(menu.getId()));
                copy.setTenantId(tenantId);
                Long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
                copy.setParentId(parentId == 0L ? 0L : idMap.getOrDefault(parentId, 0L));
                copy.setName(menu.getName());
                copy.setMenuKey(menu.getMenuKey());
                copy.setType(menu.getType());
                copy.setPath(menu.getPath());
                copy.setPerms(menu.getPerms());
                copy.setSeq(menu.getSeq());
                copy.setStatus(menu.getStatus());
                copy.setPlatformOnly(MenuBaseline.TENANT_VISIBLE);
                menuMapper.insert(copy);
                copied++;
            }
            return copied;
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
