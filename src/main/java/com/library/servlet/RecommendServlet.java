package com.library.servlet;

import com.library.dto.ApproveRequest;
import com.library.dto.RecommendRequest;
import com.library.dto.RejectRequest;
import com.library.service.RecommendService;
import com.library.util.Permission;
import com.library.util.SessionUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/recommendations", "/api/recommendations/*"})
public class RecommendServlet extends BaseServlet {

    private final RecommendService recommendService = new RecommendService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if ("mine".equals(path)) {
            SessionUser user = require(request, Permission.RECOMMEND_ADD);
            ok(response, call(() -> recommendService.mine(user.id)));
            return;
        }
        if (path.isEmpty()) {
            require(request, Permission.RECOMMEND_REVIEW);
            ok(response, call(recommendService::pending));
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if (path.isEmpty()) {
            SessionUser user = require(request, Permission.RECOMMEND_ADD);
            RecommendRequest body = readBody(request, RecommendRequest.class);
            Map<String, Object> result = call(() -> recommendService.recommend(user.id, body));
            ok(response, result);
            return;
        }
        String[] parts = segments(request);
        if (parts.length == 2 && "approve".equals(parts[1])) {
            require(request, Permission.RECOMMEND_REVIEW);
            ApproveRequest body = readBody(request, ApproveRequest.class);
            ok(response, "已通过并加入图书列表", call(() -> recommendService.approve(parts[0], body)));
            return;
        }
        if (parts.length == 2 && "reject".equals(parts[1])) {
            require(request, Permission.RECOMMEND_REVIEW);
            RejectRequest body = readBody(request, RejectRequest.class);
            call(() -> {
                recommendService.reject(parts[0], body.reason);
                return null;
            });
            ok(response, "已驳回", null);
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }
}
