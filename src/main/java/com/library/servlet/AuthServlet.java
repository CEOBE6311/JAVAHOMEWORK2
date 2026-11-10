package com.library.servlet;

import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.dto.UserVO;
import com.library.service.AuthService;
import com.library.util.SessionUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if ("login".equals(path)) {
            LoginRequest body = readBody(request, LoginRequest.class);
            Map<String, Object> data = call(() -> authService.login(body, request));
            ok(response, "登录成功", data);
            return;
        }
        if ("register".equals(path)) {
            RegisterRequest body = readBody(request, RegisterRequest.class);
            UserVO user = call(() -> authService.register(body));
            ok(response, "注册成功", user);
            return;
        }
        if ("logout".equals(path)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            ok(response, "已退出", null);
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if ("me".equals(path)) {
            SessionUser user = currentUser(request);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", call(() -> authService.me(user)));
            data.put("permissions", user.permissions);
            ok(response, data);
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }
}
