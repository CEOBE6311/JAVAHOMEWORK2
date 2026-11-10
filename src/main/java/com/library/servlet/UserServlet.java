package com.library.servlet;

import com.library.dto.UserVO;
import com.library.service.UserService;
import com.library.util.Permission;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/users/*")
public class UserServlet extends BaseServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        require(request, Permission.USER_QUERY);
        String path = path(request);
        if (path.isEmpty()) {
            List<UserVO> list = call(() -> userService.search(param(request, "keyword")));
            ok(response, list);
            return;
        }
        String[] parts = segments(request);
        if (parts.length == 1) {
            ok(response, call(() -> userService.detail(parts[0])));
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }
}
