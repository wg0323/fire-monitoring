package com.fm.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.system.entity.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 角色是配置数据，物理删除以避免软删行占用 (tenant_id, role_key) 唯一键。
     */
    @Delete("DELETE FROM sys_role WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
