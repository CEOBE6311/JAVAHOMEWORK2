package com.library.service;

import com.library.dao.BookCopyDao;
import com.library.dao.BookDao;
import com.library.dao.BookRecommendDao;
import com.library.dto.ApproveRequest;
import com.library.dto.BookVO;
import com.library.dto.RecommendRequest;
import com.library.dto.RecommendVO;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.BookRecommend;
import com.library.jdbc.JDBCTool;
import com.library.util.ApiException;
import com.library.util.DateUtil;
import com.library.util.IdUtil;
import com.library.util.ValidateUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecommendService {

    private final BookDao bookDao = new BookDao();
    private final BookCopyDao bookCopyDao = new BookCopyDao();
    private final BookRecommendDao recommendDao = new BookRecommendDao();

    public Map<String, Object> recommend(String userId, RecommendRequest request) throws Exception {
        String bookName = ValidateUtil.text(request.bookName, "书名", 1, 20);
        String author = ValidateUtil.text(request.author, "作者", 1, 20);
        String publisher = ValidateUtil.text(request.publisher, "出版社", 1, 25);
        double price = ValidateUtil.price(request.price);

        try (Connection connection = JDBCTool.getConnection()) {
            List<BookVO> existing = bookDao.list(bookName, connection);
            if (!existing.isEmpty()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("submitted", false);
                result.put("books", existing);
                return result;
            }
            if (recommendDao.existsPending(userId, bookName, connection)) {
                throw new ApiException(409, 3002, "该书名已提交过，请等待审核");
            }
        }

        LocalDateTime now = DateUtil.now();
        BookRecommend recommend = new BookRecommend();
        recommend.setId(IdUtil.uuid7());
        recommend.setCreateTime(now);
        recommend.setUpdateTime(now);
        recommend.setUserId(userId);
        recommend.setBookName(bookName);
        recommend.setAuthor(author);
        recommend.setPublisher(publisher);
        recommend.setPrice(price);
        recommend.setStatus("待审核");
        recommend.setReason(null);

        JDBCTool.transaction(connection -> {
            recommendDao.insert(recommend, connection);
            return null;
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submitted", true);
        result.put("recommend", RecommendVO.from(recommend, null));
        return result;
    }

    public List<RecommendVO> mine(String userId) throws Exception {
        try (Connection connection = JDBCTool.getConnection()) {
            return recommendDao.findMine(userId, connection).stream()
                    .map(r -> RecommendVO.from(r, null))
                    .collect(Collectors.toList());
        }
    }

    public List<RecommendVO> pending() throws Exception {
        try (Connection connection = JDBCTool.getConnection()) {
            return recommendDao.findPending(connection);
        }
    }

    public BookVO approve(String id, ApproveRequest request) throws Exception {
        String type = ValidateUtil.text(request.type, "类型", 1, 10);
        int days = ValidateUtil.intRange(request.days, "还书期限", 1, 365);
        int stock = ValidateUtil.intRange(request.stock, "库存", 0, 999);
        String introduction = ValidateUtil.optionalText(request.introduction, "简介", 512);

        LocalDateTime now = DateUtil.now();
        String bookId = JDBCTool.transaction(connection -> {
            BookRecommend recommend = recommendDao.findByIdForUpdate(id, connection);
            if (recommend == null) {
                throw new ApiException(404, 3001, "推荐记录不存在");
            }
            if (!"待审核".equals(recommend.getStatus())) {
                throw new ApiException(409, 3002, "该推荐已处理");
            }

            Book book = new Book();
            book.setId(IdUtil.uuid7());
            book.setCreateTime(now);
            book.setUpdateTime(now);
            book.setTitle(recommend.getBookName());
            book.setAuthor(recommend.getAuthor());
            book.setType(type);
            book.setPublisher(recommend.getPublisher());
            book.setDays(days);
            book.setPrice(recommend.getPrice());
            book.setStock(stock);
            book.setStatus("正常");
            book.setIntroduction(introduction);
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
            recommendDao.markApproved(id, now, connection);
            return book.getId();
        });

        return new BookService().detail(bookId);
    }

    public void reject(String id, String reason) throws Exception {
        String actualReason = ValidateUtil.text(reason, "驳回理由", 1, 100);
        LocalDateTime now = DateUtil.now();
        JDBCTool.transaction(connection -> {
            BookRecommend recommend = recommendDao.findByIdForUpdate(id, connection);
            if (recommend == null) {
                throw new ApiException(404, 3001, "推荐记录不存在");
            }
            if (!"待审核".equals(recommend.getStatus())) {
                throw new ApiException(409, 3002, "该推荐已处理");
            }
            recommendDao.markRejected(id, actualReason, now, connection);
            return null;
        });
    }
}
