package com.fm.module.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.vo.UserVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户对象转换。
 */
public final class UserConverter {

    private UserConverter() {
    }

    public static UserVO toVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    public static List<UserVO> toVOList(List<SysUser> users) {
        return users.stream().map(UserConverter::toVO).collect(Collectors.toList());
    }

    public static Page<UserVO> toVOPage(Page<SysUser> page) {
        Page<UserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }
}
