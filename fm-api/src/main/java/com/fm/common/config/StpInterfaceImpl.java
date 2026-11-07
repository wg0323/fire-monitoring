package com.fm.common.config;

import cn.dev33.satoken.stp.StpInterface;
import com.fm.module.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据源：按钮权限码来自角色-菜单配置。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final AuthService authService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return authService.permissionsByUserId(Long.valueOf(String.valueOf(loginId)));
    }

    /**
     * 当前仅用权限码校验（@SaCheckPermission / StpUtil.hasPermission），全仓检索未使用 @SaCheckRole，
     * 因此 getRoleList 返回空列表是有意的；角色信息由 AdminGuard 从会话 roleKeys 读取，不走 Sa-Token 角色校验。
     * 启用角色校验（@SaCheckRole / StpUtil.hasRole）前必须在此按会话或数据库实现真实角色列表，
     * 否则所有角色校验都会失败。
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return Collections.emptyList();
    }
}
