package com.fm.common.interceptor;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.fm.common.exception.BizException;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 平台管理员写接口拦截：租户业务数据只读。
 */
public class PlatformWriteGuardInterceptor implements HandlerInterceptor {

    private static final String USER_TYPE_PLATFORM = "PLATFORM";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!StpUtil.isLogin()) {
            return true;
        }
        SaSession session = StpUtil.getSession(false);
        if (session == null || !USER_TYPE_PLATFORM.equals(session.getString("userType"))) {
            return true;
        }
        if (!PlatformWritePolicy.isWritable(request.getMethod(), request.getRequestURI())) {
            throw new BizException(403, "平台管理员对租户业务数据只读");
        }
        return true;
    }
}
