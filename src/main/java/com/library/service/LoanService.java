package com.library.service;

import com.library.dao.BookCopyDao;
import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.dto.LoanVO;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Loan;
import com.library.jdbc.JDBCTool;
import com.library.util.ApiException;
import com.library.util.DateUtil;
import com.library.util.IdUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LoanService {

    private final BookDao bookDao = new BookDao();
    private final BookCopyDao bookCopyDao = new BookCopyDao();
    private final LoanDao loanDao = new LoanDao();

    public Map<String, Object> borrow(String userId, String bookId) throws Exception {
        if (bookId == null || bookId.isBlank()) {
            throw new ApiException(400, 1001, "请选择图书");
        }
        LocalDateTime now = DateUtil.now();
        Loan loan = JDBCTool.transaction(connection -> {
            Book book = bookDao.findByIdForUpdate(bookId, connection);
            if (book == null) {
                throw new ApiException(404, 3001, "图书不存在");
            }
            if (!"正常".equals(book.getStatus())) {
                throw new ApiException(409, 3002, "该图书已废弃");
            }
            if (book.getStock() == null || book.getStock() <= 0) {
                throw new ApiException(409, 3002, "库存不足，暂不可借");
            }
            BookCopy copy = bookCopyDao.findAvailableForUpdate(bookId, connection);
            if (copy == null) {
                throw new ApiException(409, 3002, "库存不足，暂不可借");
            }
            if (bookDao.updateStock(bookId, -1, now, connection) == 0) {
                throw new ApiException(409, 3002, "库存不足，暂不可借");
            }
            if (bookCopyDao.markBorrowed(copy.getId(), now, connection) == 0) {
                throw new ApiException(409, 3002, "该副本已被借出，请刷新后重试");
            }
            int days = book.getDays() == null || book.getDays() <= 0 ? 30 : book.getDays();
            Loan created = new Loan();
            created.setId(IdUtil.uuid7());
            created.setCreateTime(now);
            created.setUpdateTime(now);
            created.setBookCopyId(copy.getId());
            created.setUserId(userId);
            created.setBorrowDate(now);
            created.setDueDate(now.plusDays(days));
            created.setReturnDate(null);
            created.setStatus("未还");
            loanDao.insert(created, connection);
            return created;
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("loanId", loan.getId());
        result.put("copyId", loan.getBookCopyId());
        result.put("dueDate", loan.getDueDate());
        return result;
    }

    public void returnBook(String userId, String copyId) throws Exception {
        if (copyId == null || copyId.isBlank()) {
            throw new ApiException(400, 1001, "请选择要归还的副本");
        }
        LocalDateTime now = DateUtil.now();
        JDBCTool.transaction(connection -> {
            BookCopy copy = bookCopyDao.findById(copyId, connection);
            if (copy == null) {
                throw new ApiException(404, 3001, "副本不存在");
            }
            Book book = bookDao.findByIdForUpdate(copy.getBookId(), connection);
            if (book == null) {
                throw new ApiException(404, 3001, "图书不存在");
            }
            BookCopy locked = bookCopyDao.findByIdForUpdate(copyId, connection);
            if (locked == null) {
                throw new ApiException(404, 3001, "副本不存在");
            }
            Loan loan = loanDao.findActiveByCopyAndUserForUpdate(copyId, userId, connection);
            if (loan == null) {
                throw new ApiException(409, 3002, "该借阅记录不存在或已归还");
            }
            loanDao.markReturned(loan.getId(), now, connection);
            bookCopyDao.markReturned(copyId, now, connection);
            bookDao.updateStock(copy.getBookId(), 1, now, connection);
            return null;
        });
    }

    public List<LoanVO> mine(String userId, String scope, String startText, String endText) throws Exception {
        boolean unreturned = "unreturned".equals(scope);
        LocalDateTime start = DateUtil.startOfDay(startText, "开始日期");
        LocalDateTime end = DateUtil.endOfDay(endText, "结束日期");
        validateRange(start, end);
        refreshOverdue();
        try (Connection connection = JDBCTool.getConnection()) {
            return loanDao.findByUser(userId, unreturned, start, end, connection);
        }
    }

    public List<LoanVO> query(String mode, String keyword, String startText, String endText) throws Exception {
        String actualMode = mode == null || mode.isBlank() ? "all" : mode;
        if (!List.of("all", "keyword", "overdue", "range").contains(actualMode)) {
            throw new ApiException(400, 1001, "未知的查询模式");
        }
        LocalDateTime start = DateUtil.startOfDay(startText, "开始日期");
        LocalDateTime end = DateUtil.endOfDay(endText, "结束日期");
        if ("range".equals(actualMode) && (start == null || end == null)) {
            throw new ApiException(400, 1001, "请选择开始和结束日期");
        }
        validateRange(start, end);
        refreshOverdue();
        try (Connection connection = JDBCTool.getConnection()) {
            return loanDao.search(actualMode, keyword, start, end, connection);
        }
    }

    public void refreshOverdue() throws Exception {
        LocalDateTime now = DateUtil.now();
        JDBCTool.transaction(connection -> loanDao.refreshOverdue(now, connection));
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ApiException(400, 1001, "开始日期不能晚于结束日期");
        }
    }
}
