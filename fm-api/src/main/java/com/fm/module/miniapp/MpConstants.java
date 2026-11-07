package com.fm.module.miniapp;

import cn.dev33.satoken.stp.StpUtil;
import com.fm.common.context.TenantContext;

import java.util.Collection;

/**
 * 小程序端常量与角色判断：允许登录的用户类型与租户管理员判定。
 */
public final class MpConstants {

    public static final String USER_TYPE_TENANT = "TENANT";
    public static final String USER_TYPE_MERCHANT = "MERCHANT";
    public static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    public static final String ROLE_TENANT_FIELD = "TENANT_FIELD";
    public static final String ROLE_MERCHANT = "MERCHANT";

    /**
     * 小程序仅对运营中心账号与商户账号开放。
     */
    public static boolean isMiniappUserType(String userType) {
        return USER_TYPE_TENANT.equals(userType) || USER_TYPE_MERCHANT.equals(userType);
    }

    /**
     * 当前登录人是否租户管理员。
     */
    public static boolean isTenantAdmin() {
        if (!USER_TYPE_TENANT.equals(TenantContext.getUserType())) {
            return false;
        }
        try {
            if (!StpUtil.isLogin()) {
                return false;
            }
            Object roleKeys = StpUtil.getSession().get("roleKeys");
            return roleKeys instanceof Collection && ((Collection<?>) roleKeys).contains(ROLE_TENANT_ADMIN);
        } catch (Exception ex) {
            return false;
        }
    }

    private MpConstants() {
    }
}
