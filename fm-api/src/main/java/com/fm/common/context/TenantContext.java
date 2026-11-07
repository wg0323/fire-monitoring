package com.fm.common.context;

/**
 * 请求级租户上下文，由登录拦截器写入，MyBatis-Plus 租户插件读取。
 */
public final class TenantContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId, String userType, String dataScope, boolean ignoreTenant) {
        HOLDER.set(new Context(tenantId, null, null, userType, dataScope, ignoreTenant));
    }

    public static void set(Long tenantId, Long orgId, Long merchantId, String userType,
                           String dataScope, boolean ignoreTenant) {
        HOLDER.set(new Context(tenantId, orgId, merchantId, userType, dataScope, ignoreTenant));
    }

    public static Long getTenantId() {
        Context context = HOLDER.get();
        return context == null || context.tenantId == null ? 0L : context.tenantId;
    }

    public static String getUserType() {
        Context context = HOLDER.get();
        return context == null ? null : context.userType;
    }

    public static Long getOrgId() {
        Context context = HOLDER.get();
        return context == null ? null : context.orgId;
    }

    public static Long getMerchantId() {
        Context context = HOLDER.get();
        return context == null ? null : context.merchantId;
    }

    public static String getDataScope() {
        Context context = HOLDER.get();
        return context == null ? null : context.dataScope;
    }

    public static boolean isIgnoreTenant() {
        Context context = HOLDER.get();
        return context != null && context.ignoreTenant;
    }

    public static void setIgnoreTenant(boolean ignoreTenant) {
        Context context = HOLDER.get();
        if (context != null) {
            context.ignoreTenant = ignoreTenant;
        }
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static final class Context {
        private final Long tenantId;
        private final Long orgId;
        private final Long merchantId;
        private final String userType;
        private final String dataScope;
        private boolean ignoreTenant;

        private Context(Long tenantId, Long orgId, Long merchantId, String userType,
                        String dataScope, boolean ignoreTenant) {
            this.tenantId = tenantId;
            this.orgId = orgId;
            this.merchantId = merchantId;
            this.userType = userType;
            this.dataScope = dataScope;
            this.ignoreTenant = ignoreTenant;
        }
    }
}
