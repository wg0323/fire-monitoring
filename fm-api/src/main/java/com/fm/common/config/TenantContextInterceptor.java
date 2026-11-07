package com.fm.common.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.fm.common.context.TenantContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 从 Sa-Token 会话恢复租户上下文，请求结束清理。
 */
public class TenantContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (StpUtil.isLogin()) {
            SaSession session = StpUtil.getSession(false);
            if (session != null) {
                String userType = session.getString("userType");
                String dataScope = session.getString("dataScope");
                // 平台超管与 ALL 数据范围不参与租户行级过滤
                boolean ignoreTenant = "PLATFORM".equals(userType) || "ALL".equals(dataScope);
                TenantContext.set(session.getLong("tenantId"), session.getLong("orgId"),
                        session.getLong("merchantId"), userType, dataScope, ignoreTenant);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }
}
