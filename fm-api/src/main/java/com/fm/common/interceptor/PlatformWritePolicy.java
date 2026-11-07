package com.fm.common.interceptor;

import java.util.Arrays;
import java.util.List;

/**
 * 平台管理员写权限策略：平台只写平台级资源，租户业务数据一律只读。
 */
public final class PlatformWritePolicy {

    /** 平台管理员可写的后台路径前缀 */
    private static final List<String> WRITABLE_PREFIXES = Arrays.asList(
            "/api/admin/auth/",
            "/api/admin/system/",
            "/api/admin/device/dict",
            "/api/admin/upload/"
    );

    private PlatformWritePolicy() {
    }

    /**
     * 判断请求是否允许平台管理员执行。
     *
     * @param method HTTP 方法
     * @param path   请求路径
     * @return true=允许；false=应返回 403
     */
    public static boolean isWritable(String method, String path) {
        if (!isWriteMethod(method)) {
            return true;
        }
        String requestPath = path == null ? "" : path;
        for (String prefix : WRITABLE_PREFIXES) {
            if (requestPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }
}
