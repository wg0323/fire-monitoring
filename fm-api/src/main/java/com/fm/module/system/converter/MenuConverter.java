package com.fm.module.system.converter;

import com.fm.module.system.entity.SysMenu;
import com.fm.module.system.vo.MenuVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单对象转换。
 */
public final class MenuConverter {

    private MenuConverter() {
    }

    public static MenuVO toVO(SysMenu menu) {
        if (menu == null) {
            return null;
        }
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setTenantId(menu.getTenantId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setMenuKey(menu.getMenuKey());
        vo.setType(menu.getType());
        vo.setPath(menu.getPath());
        vo.setSeq(menu.getSeq());
        vo.setStatus(menu.getStatus());
        vo.setPlatformOnly(menu.getPlatformOnly());
        return vo;
    }

    public static List<MenuVO> toVOList(List<SysMenu> menus) {
        return menus.stream().map(MenuConverter::toVO).collect(Collectors.toList());
    }

    /**
     * 将扁平菜单列表组装为树，供角色授权选择。
     */
    public static List<MenuVO> toTree(List<SysMenu> menus) {
        Map<Long, MenuVO> nodeMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            nodeMap.put(menu.getId(), toVO(menu));
        }
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO node : nodeMap.values()) {
            MenuVO parent = node.getParentId() == null ? null : nodeMap.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
