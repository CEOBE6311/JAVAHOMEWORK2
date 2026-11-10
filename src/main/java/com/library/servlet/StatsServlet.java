package com.library.servlet;

import com.library.service.StatsService;
import com.library.util.Permission;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/stats/*")
public class StatsServlet extends BaseServlet {

    private final StatsService statsService = new StatsService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        require(request, Permission.LOAN_STATS);
        String path = path(request);
        if ("by-type".equals(path)) {
            ok(response, call(statsService::byType));
            return;
        }
        if ("by-major".equals(path)) {
            ok(response, call(statsService::byMajor));
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }
}
