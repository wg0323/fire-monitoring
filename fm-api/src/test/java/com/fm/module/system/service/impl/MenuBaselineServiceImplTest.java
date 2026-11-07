package com.fm.module.system.service.impl;

import com.fm.module.system.baseline.MenuBaseline;
import com.fm.module.system.entity.SysMenu;
import com.fm.module.system.mapper.SysMenuMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 菜单基线复制测试：脱钩、排除 platform_only。
 */
class MenuBaselineServiceImplTest {

    private final SysMenuMapper menuMapper = mock(SysMenuMapper.class);
    private final MenuBaselineServiceImpl service = new MenuBaselineServiceImpl(menuMapper);

    @Test
    void copyBaselineSkipsPlatformOnlyAndRemapsParents() {
        when(menuMapper.selectCount(any())).thenReturn(0L);
        SysMenu dir = menu(1001L, 0L, "权限管理", "system", MenuBaseline.TYPE_DIR, 0);
        SysMenu page = menu(1125L, 1001L, "用户管理", "system:user", MenuBaseline.TYPE_MENU, 0);
        SysMenu platformPage = menu(1127L, 1001L, "运营中心管理", "system:tenant",
                MenuBaseline.TYPE_MENU, MenuBaseline.PLATFORM_ONLY);
        when(menuMapper.selectList(any())).thenReturn(Arrays.asList(dir, page, platformPage));

        int copied = service.copyBaseline(9L);

        assertEquals(2, copied);
        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(menuMapper, times(2)).insert(captor.capture());
        List<SysMenu> copies = captor.getAllValues();
        assertTrue(copies.stream().allMatch(item -> item.getTenantId().equals(9L)));
        assertTrue(copies.stream().allMatch(item -> item.getPlatformOnly() == 0));
        SysMenu dirCopy = copies.stream().filter(item -> "system".equals(item.getMenuKey()))
                .findFirst().orElseThrow(AssertionError::new);
        SysMenu pageCopy = copies.stream().filter(item -> "system:user".equals(item.getMenuKey()))
                .findFirst().orElseThrow(AssertionError::new);
        assertNotEquals(1001L, dirCopy.getId());
        assertEquals(dirCopy.getId(), pageCopy.getParentId());
    }

    @Test
    void existingTenantMenusAreNotOverwritten() {
        when(menuMapper.selectCount(any())).thenReturn(3L);

        assertEquals(0, service.copyBaseline(9L));
        verify(menuMapper, never()).selectList(any());
    }

    private SysMenu menu(Long id, Long parentId, String name, String menuKey, int type, int platformOnly) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setTenantId(0L);
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setMenuKey(menuKey);
        menu.setType(type);
        menu.setSeq(1);
        menu.setStatus(1);
        menu.setPlatformOnly(platformOnly);
        return menu;
    }
}
