package com.library.servlet;

import com.library.service.BookService;
import com.library.util.Permission;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/copies/*")
public class BookCopyServlet extends BaseServlet {

    private final BookService bookService = new BookService();

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        require(request, Permission.COPY_EDIT);
        String[] parts = segments(request);
        if (parts.length != 1) {
            error(response, 404, 3001, "接口不存在");
            return;
        }
        call(() -> {
            bookService.deleteCopy(parts[0]);
            return null;
        });
        ok(response, "删除成功", null);
    }
}
