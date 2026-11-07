package com.fm.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.system.dto.MenuSaveDTO;
import com.fm.module.system.entity.SysMenu;

import java.util.List;

/**
 * 菜单服务：查询与维护均按 tenant_id 隔离。
 */
public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> listTreeNodes();

    Long create(MenuSaveDTO request);

    void update(Long id, MenuSaveDTO request);

    void delete(Long id);
}
