package com.fm.module.system.support;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.fm.common.exception.BizException;
import com.fm.module.system.entity.SysRole;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统管理写操作的角色守卫。
 */
public final class AdminGuard {

    public static final String USER_TYPE_PLATFORM = "PLATFORM";
    public static final String ROLE_KEYS_SESSION = "roleKeys";

    private AdminGuard() {
    }

    public static boolean isPlatform() {
        return USER_TYPE_PLATFORM.equals(userType());
    }

    public static boolean isTenantAdmin() {
        return roleKeys().contains(SysRole.TENANT_ADMIN);
    }

    public static void requireAdmin() {
        if (!isPlatform() && !isTenantAdmin()) {
            throw new BizException(403, "仅平台管理员或租户管理员可操作");
        }
    }

    public static void requirePlatform() {
        if (!isPlatform()) {
            throw new BizException(403, "仅平台管理员可操作");
        }
    }

    public static String userType() {
        SaSession session = session();
        return session == null ? null : session.getString("userType");
    }

    public static List<String> roleKeys() {
        SaSession session = session();
        if (session == null) {
            return Collections.emptyList();
        }
        Object value = session.get(ROLE_KEYS_SESSION);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static SaSession session() {
        return StpUtil.isLogin() ? StpUtil.getSession(false) : null;
    }
}
