package com.fm.module.system.service;

import com.fm.module.system.dto.LoginDTO;
import com.fm.module.system.dto.PasswordChangeDTO;
import com.fm.module.system.vo.LoginVO;
import com.fm.module.system.vo.MenuVO;
import com.fm.module.system.vo.UserInfoVO;

import java.util.List;
import java.util.Set;

/**
 * 登录与会话编排服务。
 */
public interface AuthService {

    LoginVO login(LoginDTO request, String ip, String device, Set<String> allowedRoleKeys);

    LoginVO loginByUserId(Long userId, String ip, String device, Set<String> allowedRoleKeys);

    void logout();

    void switchMerchant(Long merchantId);

    void changePassword(PasswordChangeDTO request);

    UserInfoVO info();

    List<MenuVO> menus();

    List<String> permissionsByUserId(Long userId);
}
