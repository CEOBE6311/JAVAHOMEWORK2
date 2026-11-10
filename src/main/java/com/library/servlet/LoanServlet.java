package com.library.servlet;

import com.library.dto.BorrowRequest;
import com.library.dto.LoanVO;
import com.library.dto.ReturnRequest;
import com.library.service.LoanService;
import com.library.util.Permission;
import com.library.util.SessionUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/loans", "/api/loans/*"})
public class LoanServlet extends BaseServlet {

    private final LoanService loanService = new LoanService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if ("mine".equals(path)) {
            SessionUser user = require(request, Permission.LOAN_SELF);
            List<LoanVO> list = call(() -> loanService.mine(
                    user.id, param(request, "scope"), param(request, "start"), param(request, "end")));
            ok(response, list);
            return;
        }
        if (path.isEmpty()) {
            require(request, Permission.LOAN_QUERY);
            List<LoanVO> list = call(() -> loanService.query(
                    param(request, "mode"), param(request, "keyword"),
                    param(request, "start"), param(request, "end")));
            ok(response, list);
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if (path.isEmpty()) {
            SessionUser user = require(request, Permission.LOAN_BORROW);
            BorrowRequest body = readBody(request, BorrowRequest.class);
            Map<String, Object> data = call(() -> loanService.borrow(user.id, body.bookId));
            ok(response, "借阅成功", data);
            return;
        }
        if ("return".equals(path)) {
            SessionUser user = require(request, Permission.LOAN_RETURN);
            ReturnRequest body = readBody(request, ReturnRequest.class);
            call(() -> {
                loanService.returnBook(user.id, body.copyId);
                return null;
            });
            ok(response, "归还成功", null);
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }
}
