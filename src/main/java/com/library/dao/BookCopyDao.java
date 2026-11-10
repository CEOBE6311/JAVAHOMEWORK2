package com.library.dao;

import com.library.dto.BookCopyVO;
import com.library.entity.BookCopy;
import com.library.jdbc.JDBCTool;
import com.library.jdbc.RowMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

public class BookCopyDao {

    private static final String COLUMNS = "id, createTime, updateTime, bookId, isBorrowed";

    private static final RowMapper<BookCopy> ENTITY_MAPPER = BookCopyDao::mapCopy;
    private static final RowMapper<BookCopyVO> VO_MAPPER = BookCopyDao::mapVO;

    public BookCopy findById(String id, Connection connection) throws Exception {
        return JDBCTool.queryOne(connection,
                "SELECT " + COLUMNS + " FROM bookcopy WHERE id = ? LIMIT 1",
                ENTITY_MAPPER, id);
    }

    public BookCopy findByIdForUpdate(String id, Connection connection) throws Exception {
        return JDBCTool.queryOne(connection,
                "SELECT " + COLUMNS + " FROM bookcopy WHERE id = ? FOR UPDATE",
                ENTITY_MAPPER, id);
    }

    public BookCopy findAvailableForUpdate(String bookId, Connection connection) throws Exception {
        return JDBCTool.queryOne(connection,
                "SELECT " + COLUMNS + " FROM bookcopy "
                        + "WHERE bookId = ? AND isBorrowed = 0 "
                        + "ORDER BY createTime ASC, id ASC LIMIT 1 FOR UPDATE",
                ENTITY_MAPPER, bookId);
    }

    public List<BookCopyVO> listByBook(String bookId, Connection connection) throws Exception {
        String sql = "SELECT c.id, c.createTime, c.updateTime, c.bookId, c.isBorrowed, b.title AS bookTitle "
                + "FROM bookcopy c JOIN book b ON b.id = c.bookId "
                + "WHERE c.bookId = ? ORDER BY c.createTime ASC, c.id ASC";
        return JDBCTool.queryList(connection, sql, VO_MAPPER, bookId);
    }

    public void insert(BookCopy copy, Connection connection) throws Exception {
        JDBCTool.save(copy, connection);
    }

    public int markBorrowed(String copyId, LocalDateTime now, Connection connection) throws Exception {
        return JDBCTool.executeUpdate(connection,
                "UPDATE bookcopy SET isBorrowed = 1, updateTime = ? WHERE id = ? AND isBorrowed = 0",
                now, copyId);
    }

    public int markReturned(String copyId, LocalDateTime now, Connection connection) throws Exception {
        return JDBCTool.executeUpdate(connection,
                "UPDATE bookcopy SET isBorrowed = 0, updateTime = ? WHERE id = ?",
                now, copyId);
    }

    public int countBorrowed(String bookId, Connection connection) throws Exception {
        Integer count = JDBCTool.queryOne(connection,
                "SELECT COUNT(*) FROM bookcopy WHERE bookId = ? AND isBorrowed = 1",
                rs -> rs.getInt(1), bookId);
        return count == null ? 0 : count;
    }

    public int countLoans(String copyId, Connection connection) throws Exception {
        Integer count = JDBCTool.queryOne(connection,
                "SELECT COUNT(*) FROM loan WHERE bookCopyId = ?",
                rs -> rs.getInt(1), copyId);
        return count == null ? 0 : count;
    }

    public int delete(String copyId, Connection connection) throws Exception {
        return JDBCTool.executeUpdate(connection, "DELETE FROM bookcopy WHERE id = ?", copyId);
    }

    private static BookCopy mapCopy(ResultSet rs) throws java.sql.SQLException {
        BookCopy copy = new BookCopy();
        copy.setId(rs.getString("id"));
        copy.setCreateTime(toDateTime(rs.getTimestamp("createTime")));
        copy.setUpdateTime(toDateTime(rs.getTimestamp("updateTime")));
        copy.setBookId(rs.getString("bookId"));
        copy.setBorrowed(rs.getBoolean("isBorrowed"));
        return copy;
    }

    private static BookCopyVO mapVO(ResultSet rs) throws java.sql.SQLException {
        BookCopyVO vo = new BookCopyVO();
        vo.id = rs.getString("id");
        vo.bookId = rs.getString("bookId");
        vo.bookTitle = rs.getString("bookTitle");
        vo.isBorrowed = rs.getBoolean("isBorrowed");
        vo.createTime = toDateTime(rs.getTimestamp("createTime"));
        return vo;
    }

    private static LocalDateTime toDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
