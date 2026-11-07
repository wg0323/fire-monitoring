package com.fm.module.system.converter;

import com.fm.module.system.entity.SysRole;
import com.fm.module.system.vo.RoleVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色对象转换。
 */
public final class RoleConverter {

    private RoleConverter() {
    }

    public static RoleVO toVO(SysRole role) {
        if (role == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }

    public static List<RoleVO> toVOList(List<SysRole> roles) {
        return roles.stream().map(RoleConverter::toVO).collect(Collectors.toList());
    }
}
