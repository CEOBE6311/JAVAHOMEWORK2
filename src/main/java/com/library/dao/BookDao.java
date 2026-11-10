package com.library.dao;

import com.library.dto.BookVO;
import com.library.entity.Book;
import com.library.jdbc.JDBCTool;
import com.library.jdbc.RowMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookDao {

    private static final String COLUMNS = "id, createTime, updateTime, title, author, type, publisher, "
            + "days, price, stock, status, introduction";

    private static final String VO_COLUMNS = "b.id, b.createTime, b.updateTime, b.title, b.author, b.type, "
            + "b.publisher, b.days, b.price, b.stock, b.status, b.introduction, "
            + "(SELECT COUNT(*) FROM bookcopy c WHERE c.bookId = b.id) AS totalCopies, "
            + "(SELECT COUNT(*) FROM bookcopy c WHERE c.bookId = b.id AND c.isBorrowed = 1) AS borrowedCopies";

    private static final RowMapper<Book> ENTITY_MAPPER = BookDao::mapBook;
    private static final RowMapper<BookVO> VO_MAPPER = BookDao::mapVO;

    public Book findById(String id, Connection connection) throws Exception {
        String sql = "SELECT " + COLUMNS + " FROM book WHERE id = ? LIMIT 1";
        return JDBCTool.queryOne(connection, sql, ENTITY_MAPPER, id);
    }

    public Book findByIdForUpdate(String id, Connection connection) throws Exception {
        String sql = "SELECT " + COLUMNS + " FROM book WHERE id = ? FOR UPDATE";
        return JDBCTool.queryOne(connection, sql, ENTITY_MAPPER, id);
    }

    public BookVO findVOById(String id, Connection connection) throws Exception {
        String sql = "SELECT " + VO_COLUMNS + " FROM book b WHERE b.id = ?";
        return JDBCTool.queryOne(connection, sql, VO_MAPPER, id);
    }

    public List<BookVO> list(String keyword, Connection connection) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT ").append(VO_COLUMNS).append(" FROM book b");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + escapeLike(keyword.trim()) + "%";
            sql.append(" WHERE b.title LIKE ? OR b.author LIKE ? OR b.type LIKE ? OR b.publisher LIKE ?");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY b.createTime DESC, b.id DESC");
        return JDBCTool.queryList(connection, sql.toString(), VO_MAPPER, args.toArray());
    }

    public List<BookVO> borrowable(Connection connection) throws Exception {
        String sql = "SELECT " + VO_COLUMNS + " FROM book b "
                + "WHERE b.status = '正常' AND b.stock > 0 "
                + "ORDER BY b.createTime DESC, b.id DESC";
        return JDBCTool.queryList(connection, sql, VO_MAPPER);
    }

    public void insert(Book book, Connection connection) throws Exception {
        JDBCTool.save(book, connection);
    }

    /** 只更新题目允许编辑的 6 个字段，绝不覆盖 stock/status。 */
    public void updateEditable(Book book, Connection connection) throws Exception {
        String sql = "UPDATE book SET title = ?, author = ?, type = ?, publisher = ?, "
                + "days = ?, price = ?, updateTime = ? WHERE id = ?";
        JDBCTool.executeUpdate(connection, sql,
                book.getTitle(), book.getAuthor(), book.getType(), book.getPublisher(),
                book.getDays(), book.getPrice(), book.getUpdateTime(), book.getId());
    }

    public void updateStatus(String id, String status, LocalDateTime now, Connection connection) throws Exception {
        JDBCTool.executeUpdate(connection,
                "UPDATE book SET status = ?, updateTime = ? WHERE id = ?",
                status, now, id);
    }

    public int updateStock(String bookId, int delta, LocalDateTime now, Connection connection) throws Exception {
        if (delta < 0) {
            return JDBCTool.executeUpdate(connection,
                    "UPDATE book SET stock = stock - 1, updateTime = ? WHERE id = ? AND stock > 0",
                    now, bookId);
        }
        return JDBCTool.executeUpdate(connection,
                "UPDATE book SET stock = stock + 1, updateTime = ? WHERE id = ?",
                now, bookId);
    }

    private static Book mapBook(ResultSet rs) throws java.sql.SQLException {
        Book book = new Book();
        book.setId(rs.getString("id"));
        book.setCreateTime(toDateTime(rs.getTimestamp("createTime")));
        book.setUpdateTime(toDateTime(rs.getTimestamp("updateTime")));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setType(rs.getString("type"));
        book.setPublisher(rs.getString("publisher"));
        book.setDays((Integer) rs.getObject("days"));
        book.setPrice(rs.getObject("price") == null ? null : rs.getDouble("price"));
        book.setStock((Integer) rs.getObject("stock"));
        book.setStatus(rs.getString("status"));
        book.setIntroduction(rs.getString("introduction"));
        return book;
    }

    private static BookVO mapVO(ResultSet rs) throws java.sql.SQLException {
        BookVO vo = new BookVO();
        vo.id = rs.getString("id");
        vo.createTime = toDateTime(rs.getTimestamp("createTime"));
        vo.updateTime = toDateTime(rs.getTimestamp("updateTime"));
        vo.title = rs.getString("title");
        vo.author = rs.getString("author");
        vo.type = rs.getString("type");
        vo.publisher = rs.getString("publisher");
        vo.days = (Integer) rs.getObject("days");
        vo.price = rs.getObject("price") == null ? null : rs.getDouble("price");
        vo.stock = (Integer) rs.getObject("stock");
        vo.status = rs.getString("status");
        vo.introduction = rs.getString("introduction");
        vo.totalCopies = rs.getInt("totalCopies");
        vo.borrowedCopies = rs.getInt("borrowedCopies");
        return vo;
    }

    private static LocalDateTime toDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
