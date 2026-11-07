package com.fm.common.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 生产环境屏蔽接口文档端点。
 *
 * <p>knife4j 自带的 production 开关在 4.5.0 下不会拦截 /doc.html 与 /v3/api-docs，
 * 且这两个路径不经过 MVC 拦截器链，因此改为 Servlet Filter：knife4j.production=true 时
 * 文档路径一律返回 404。
 */
@Component
public class DocAccessGuardFilter extends OncePerRequestFilter {

    private static final String[] DOC_PATH_PREFIXES = {"/doc.html", "/v3/api-docs", "/swagger-ui"};

    private final boolean production;

    public DocAccessGuardFilter(@Value("${knife4j.production:false}") boolean production) {
        this.production = production;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (production && isDocRequest(request.getRequestURI())) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isDocRequest(String uri) {
        String path = uri == null ? "" : uri;
        for (String prefix : DOC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
