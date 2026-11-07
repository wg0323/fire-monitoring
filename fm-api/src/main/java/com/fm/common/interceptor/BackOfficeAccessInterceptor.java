package com.fm.common.interceptor;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.fm.common.exception.BizException;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 后台端访问限制：外勤与商户只能登录小程序，禁止访问后台接口。
 */
public class BackOfficeAccessInterceptor implements HandlerInterceptor {

    private static final String USER_TYPE_MERCHANT = "MERCHANT";
    private static final String SCOPE_WORK_AREA = "WORK_AREA";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!StpUtil.isLogin()) {
            return true;
        }
        SaSession session = StpUtil.getSession(false);
        if (session == null) {
            return true;
        }
        String userType = session.getString("userType");
        String dataScope = session.getString("dataScope");
        if (USER_TYPE_MERCHANT.equals(userType) || SCOPE_WORK_AREA.equals(dataScope)) {
            throw new BizException(403, "当前账号仅可访问小程序");
        }
        return true;
    }
}
