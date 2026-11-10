package com.library.service;

import com.library.dao.BookCopyDao;
import com.library.dao.BookDao;
import com.library.dto.BookCopyVO;
import com.library.dto.BookRequest;
import com.library.dto.BookVO;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.jdbc.JDBCTool;
import com.library.util.ApiException;
import com.library.util.DateUtil;
import com.library.util.IdUtil;
import com.library.util.ValidateUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class BookService {

    private final BookDao bookDao = new BookDao();
    private final BookCopyDao bookCopyDao = new BookCopyDao();

    public List<BookVO> list(String keyword) throws Exception {
        try (Connection connection = JDBCTool.getConnection()) {
            return bookDao.list(keyword, connection);
        }
    }

    public List<BookVO> borrowable() throws Exception {
        try (Connection connection = JDBCTool.getConnection()) {
            return bookDao.borrowable(connection);
        }
    }

    public BookVO detail(String id) throws Exception {
        try (Connection connection = JDBCTool.getConnection()) {
            BookVO vo = bookDao.findVOById(id, connection);
            if (vo == null) {
                throw new ApiException(404, 3001, "图书不存在");
            }
            return vo;
        }
    }

    public BookVO add(BookRequest request) throws Exception {
        String title = ValidateUtil.text(request.title, "书名", 1, 30);
        String author = ValidateUtil.text(request.author, "作者", 1, 30);
        String type = ValidateUtil.text(request.type, "类型", 1, 10);
        String publisher = ValidateUtil.text(request.publisher, "出版社", 1, 20);
        int days = ValidateUtil.intRange(request.days == null ? 30 : request.days, "还书期限", 1, 365);
        double price = ValidateUtil.price(request.price == null ? 0D : request.price);
        int stock = ValidateUtil.intRange(request.stock == null ? 0 : request.stock, "库存", 0, 999);
        String introduction = ValidateUtil.optionalText(request.introduction, "简介", 512);

        LocalDateTime now = DateUtil.now();
        Book book = new Book();
        book.setId(IdUtil.uuid7());
        book.setCreateTime(now);
        book.setUpdateTime(now);
        book.setTitle(title);
        book.setAuthor(author);
        book.setType(type);
        book.setPublisher(publisher);
        book.setDays(days);
        book.setPrice(price);
        book.setStock(stock);
        book.setStatus("正常");
        book.setIntroduction(introduction);

        JDBCTool.transaction(connection -> {
            bookDao.insert(book, connection);
            for (int i = 0; i < stock; i++) {
                BookCopy copy = new BookCopy();
                copy.setId(IdUtil.uuid7());
                copy.setCreateTime(now);
                copy.setUpdateTime(now);
                copy.setBookId(book.getId());
                copy.setBorrowed(false);
                bookCopyDao.insert(copy, connection);
            }
            return null;
        });
        return detail(book.getId());
    }

    public BookVO edit(String id, BookRequest request) throws Exception {
        String title = ValidateUtil.text(request.title, "书名", 1, 30);
        String author = ValidateUtil.text(request.author, "作者", 1, 30);
        String type = ValidateUtil.text(request.type, "类型", 1, 10);
        String publisher = ValidateUtil.text(request.publisher, "出版社", 1, 20);
        int days = ValidateUtil.intRange(request.days, "还书期限", 1, 365);
        double price = ValidateUtil.price(request.price);

        JDBCTool.transaction(connection -> {
            Book book = bookDao.findById(id, connection);
            if (book == null) {
                throw new ApiException(404, 3001, "图书不存在");
            }
            if ("已废弃".equals(book.getStatus())) {
                throw new ApiException(409, 3002, "已废弃图书不可编辑");
            }
            book.setTitle(title);
            book.setAuthor(author);
            book.setType(type);
            book.setPublisher(publisher);
            book.setDays(days);
            book.setPrice(price);
            book.setUpdateTime(DateUtil.now());
            bookDao.updateEditable(book, connection);
            return null;
        });
        return detail(id);
    }

    public void discard(String id) throws Exception {
        JDBCTool.transaction(connection -> {
            Book book = bookDao.findByIdForUpdate(id, connection);
            if (book == null) {
                throw new ApiException(404, 3001, "图书不存在");
            }
            if ("已废弃".equals(book.getStatus())) {
                throw new ApiException(409, 3002, "该图书已经废弃");
            }
            int borrowed = bookCopyDao.countBorrowed(id, connection);
            if (borrowed > 0) {
                throw new ApiException(409, 3002, "存在未归还副本，不能废弃（外借 " + borrowed + " 本）");
            }
            bookDao.updateStatus(id, "已废弃", DateUtil.now(), connection);
            return null;
        });
    }

    public List<BookCopyVO> listCopies(String bookId) throws Exception {
        try (Connection connection = JDBCTool.getConnection()) {
            if (bookDao.findById(bookId, connection) == null) {
                throw new ApiException(404, 3001, "图书不存在");
            }
            return bookCopyDao.listByBook(bookId, connection);
        }
    }

    public BookCopyVO addCopy(String bookId) throws Exception {
        LocalDateTime now = DateUtil.now();
        BookCopy copy = JDBCTool.transaction(connection -> {
            Book book = bookDao.findByIdForUpdate(bookId, connection);
            if (book == null) {
                throw new ApiException(404, 3001, "图书不存在");
            }
            if (!"正常".equals(book.getStatus())) {
                throw new ApiException(409, 3002, "已废弃图书不能新增副本");
            }
            BookCopy created = new BookCopy();
            created.setId(IdUtil.uuid7());
            created.setCreateTime(now);
            created.setUpdateTime(now);
            created.setBookId(bookId);
            created.setBorrowed(false);
            bookCopyDao.insert(created, connection);
            bookDao.updateStock(bookId, 1, now, connection);
            return created;
        });
        try (Connection connection = JDBCTool.getConnection()) {
            Book book = bookDao.findById(bookId, connection);
            return BookCopyVO.from(copy, book == null ? null : book.getTitle());
        }
    }

    public void deleteCopy(String copyId) throws Exception {
        JDBCTool.transaction(connection -> {
            BookCopy copy = bookCopyDao.findById(copyId, connection);
            if (copy == null) {
                throw new ApiException(404, 3001, "副本不存在");
            }
            // 全局锁顺序：book -> bookcopy
            Book book = bookDao.findByIdForUpdate(copy.getBookId(), connection);
            if (book == null) {
                throw new ApiException(404, 3001, "图书不存在");
            }
            if (!"正常".equals(book.getStatus())) {
                throw new ApiException(409, 3002, "已废弃图书不能删除副本");
            }
            BookCopy locked = bookCopyDao.findByIdForUpdate(copyId, connection);
            if (locked == null) {
                throw new ApiException(404, 3001, "副本不存在");
            }
            if (locked.isBorrowed()) {
                throw new ApiException(409, 3002, "该副本已借出，不能删除");
            }
            if (bookCopyDao.countLoans(copyId, connection) > 0) {
                throw new ApiException(409, 3002, "该副本存在借阅记录，不能删除");
            }
            bookCopyDao.delete(copyId, connection);
            bookDao.updateStock(copy.getBookId(), -1, DateUtil.now(), connection);
            return null;
        });
    }
}
