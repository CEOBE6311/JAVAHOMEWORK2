package com.library.filter;

import com.library.util.ApiResult;
import com.library.util.JsonUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/** 登录检查与 API 401/页面跳转。 */
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String context = request.getContextPath();
        String path = request.getRequestURI().substring(context.length());
        if (path.isEmpty()) {
            path = "/";
        }

        if (path.startsWith("/static/") || "/favicon.ico".equals(path)) {
            chain.doFilter(req, res);
            return;
        }

        boolean api = path.startsWith("/api/");
        boolean publicApi = "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/captcha".equals(path);

        if (api && publicApi) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        boolean logged = session != null && session.getAttribute("loginUser") != null;

        if (api) {
            if (!logged) {
                writeUnauthorized(response);
                return;
            }
            chain.doFilter(req, res);
            return;
        }

        if ("/login.html".equals(path) || "/register.html".equals(path)) {
            if (logged) {
                response.sendRedirect(context + "/index.html");
                return;
            }
            chain.doFilter(req, res);
            return;
        }

        if ("/".equals(path) || "/index.html".equals(path)) {
            if (!logged) {
                response.sendRedirect(context + "/login.html");
                return;
            }
            if ("/".equals(path)) {
                response.sendRedirect(context + "/index.html");
                return;
            }
            chain.doFilter(req, res);
            return;
        }

        chain.doFilter(req, res);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JsonUtil.toJson(ApiResult.error(2001, "请先登录")));
        }
    }
}
