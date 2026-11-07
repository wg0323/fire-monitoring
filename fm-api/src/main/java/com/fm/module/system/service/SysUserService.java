package com.fm.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.system.dto.UserQueryDTO;
import com.fm.module.system.dto.UserSaveDTO;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.vo.UserVO;

import java.util.List;

/**
 * 用户服务：平台可管理全部租户账号，租户管理员只管理本租户账号。
 */
public interface SysUserService extends IService<SysUser> {

    Page<UserVO> page(UserQueryDTO query);

    UserVO detail(Long id);

    Long create(UserSaveDTO request);

    void update(Long id, UserSaveDTO request);

    void delete(Long id);

    void resetPassword(Long id, String password);

    void bindWorkAreas(Long id, List<Long> workAreaIds);

    List<UserVO> options(Long tenantId, String userType);
}
