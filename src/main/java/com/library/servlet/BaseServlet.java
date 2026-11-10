package com.library.servlet;

import com.library.util.ApiException;
import com.library.util.ApiResult;
import com.library.util.JsonUtil;
import com.library.util.Permission;
import com.library.util.SessionUser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/** Servlet 基类：统一 JSON、异常、登录与权限。 */
public abstract class BaseServlet extends HttpServlet {

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    protected <T> T call(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            super.service(request, response);
        } catch (ApiException e) {
            writeError(response, e.getStatus(), e.getCode(), e.getMessage());
        } catch (Exception e) {
            log("请求处理失败: " + request.getMethod() + " " + request.getRequestURI(), e);
            writeError(response, 500, 5000, "服务器内部错误");
        }
    }

    protected String path(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || "/".equals(path)) {
            return "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    protected String[] segments(HttpServletRequest request) {
        String path = path(request);
        if (path.isEmpty()) {
            return new String[0];
        }
        String clean = path.startsWith("/") ? path.substring(1) : path;
        return clean.isEmpty() ? new String[0] : clean.split("/");
    }

    protected SessionUser currentUser(HttpServletRequest request) {
        Object value = request.getSession(false) == null
                ? null : request.getSession(false).getAttribute("loginUser");
        if (!(value instanceof SessionUser user)) {
            throw new ApiException(401, 2001, "请先登录");
        }
        return user;
    }

    protected SessionUser require(HttpServletRequest request, String permission) {
        SessionUser user = currentUser(request);
        Permission.require(user, permission);
        return user;
    }

    protected <T> T readBody(HttpServletRequest request, Class<T> type) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = request.getReader().lines().reduce("", (a, b) -> a + b);
        if (body == null || body.isBlank()) {
            throw new ApiException(400, 1001, "请求体不能为空");
        }
        try {
            T value = JsonUtil.fromJson(body, type);
            if (value == null) {
                throw new ApiException(400, 1001, "请求体不能为空");
            }
            return value;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(400, 1001, "请求参数格式不正确");
        }
    }

    protected String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    protected void ok(HttpServletResponse response, Object data) throws IOException {
        write(response, 200, ApiResult.ok(data));
    }

    protected void ok(HttpServletResponse response, String message, Object data) throws IOException {
        write(response, 200, ApiResult.ok(message, data));
    }

    protected void error(HttpServletResponse response, int status, int code, String message) throws IOException {
        writeError(response, status, code, message);
    }

    private void writeError(HttpServletResponse response, int status, int code, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        write(response, status, ApiResult.error(code, message));
    }

    private void write(HttpServletResponse response, int status, ApiResult<?> result) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JsonUtil.toJson(result));
        }
    }
}
