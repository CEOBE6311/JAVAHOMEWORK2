package com.library.servlet;

import com.library.dto.BookRequest;
import com.library.dto.BookVO;
import com.library.service.BookService;
import com.library.util.Permission;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/books", "/api/books/*"})
public class BookServlet extends BaseServlet {

    private final BookService bookService = new BookService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if (path.isEmpty()) {
            require(request, Permission.BOOK_READ);
            List<BookVO> list = call(() -> bookService.list(param(request, "keyword")));
            ok(response, list);
            return;
        }
        if ("borrowable".equals(path)) {
            require(request, Permission.LOAN_BORROW);
            List<BookVO> list = call(bookService::borrowable);
            ok(response, list);
            return;
        }
        String[] parts = segments(request);
        if (parts.length == 1) {
            require(request, Permission.BOOK_READ);
            ok(response, call(() -> bookService.detail(parts[0])));
            return;
        }
        if (parts.length == 2 && "copies".equals(parts[1])) {
            require(request, Permission.COPY_EDIT);
            ok(response, call(() -> bookService.listCopies(parts[0])));
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if (path.isEmpty()) {
            require(request, Permission.BOOK_ADD);
            BookRequest body = readBody(request, BookRequest.class);
            ok(response, "添加成功", call(() -> bookService.add(body)));
            return;
        }
        String[] parts = segments(request);
        if (parts.length == 2 && "discard".equals(parts[1])) {
            require(request, Permission.BOOK_DISCARD);
            call(() -> {
                bookService.discard(parts[0]);
                return null;
            });
            ok(response, "已废弃", null);
            return;
        }
        if (parts.length == 2 && "copies".equals(parts[1])) {
            require(request, Permission.COPY_EDIT);
            ok(response, "新增副本成功", call(() -> bookService.addCopy(parts[0])));
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] parts = segments(request);
        if (parts.length == 1) {
            require(request, Permission.BOOK_EDIT);
            BookRequest body = readBody(request, BookRequest.class);
            ok(response, "保存成功", call(() -> bookService.edit(parts[0], body)));
            return;
        }
        error(response, 404, 3001, "接口不存在");
    }
}
