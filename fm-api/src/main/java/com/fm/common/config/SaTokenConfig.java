package com.fm.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.fm.common.interceptor.BackOfficeAccessInterceptor;
import com.fm.common.interceptor.PlatformWriteGuardInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 登录校验与租户上下文拦截器。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 先恢复租户上下文，再执行登录与按钮权限校验
        registry.addInterceptor(new TenantContextInterceptor())
                .addPathPatterns("/api/admin/**", "/api/mp/**");
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/admin/**", "/api/mp/**")
                // /api/mp/auth/login 在 MpAuthController 中不存在，属无效白名单，已删除；
                // mp 端实际免登录入口为 wx-login / phone-login。
                .excludePathPatterns("/api/admin/auth/login",
                        "/api/mp/auth/wx-login", "/api/mp/auth/phone-login");
        // PlatformWritePolicy 的可写白名单全部是 /api/admin/... 前缀，对 /api/mp/** 为纯空转，
        // 且可能误伤小程序端写请求；按策略实际覆盖范围收窄为 /api/admin/**，不扩展策略。
        registry.addInterceptor(new PlatformWriteGuardInterceptor())
                .addPathPatterns("/api/admin/**");
        registry.addInterceptor(new BackOfficeAccessInterceptor())
                .addPathPatterns("/api/admin/**");
    }
}
