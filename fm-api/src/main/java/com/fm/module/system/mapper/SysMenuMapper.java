package com.fm.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.system.entity.SysMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 菜单是配置数据，物理删除以避免软删行占用 (tenant_id, menu_key) 唯一键。
     */
    @Delete("DELETE FROM sys_menu WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
