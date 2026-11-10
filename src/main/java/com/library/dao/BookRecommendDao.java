package com.library.dao;

import com.library.dto.RecommendVO;
import com.library.entity.BookRecommend;
import com.library.jdbc.JDBCTool;
import com.library.jdbc.RowMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

public class BookRecommendDao {

    private static final String COLUMNS = "id, createTime, updateTime, userId, bookName, author, "
            + "publisher, price, status, reason";

    private static final RowMapper<BookRecommend> MAPPER = BookRecommendDao::mapRecommend;

    public void insert(BookRecommend recommend, Connection connection) throws Exception {
        JDBCTool.save(recommend, connection);
    }

    public boolean existsPending(String userId, String bookName, Connection connection) throws Exception {
        Integer count = JDBCTool.queryOne(connection,
                "SELECT COUNT(*) FROM bookrecommend WHERE userId = ? AND bookName = ? AND status = '待审核'",
                rs -> rs.getInt(1), userId, bookName);
        return count != null && count > 0;
    }

    public List<BookRecommend> findMine(String userId, Connection connection) throws Exception {
        String sql = "SELECT " + COLUMNS + " FROM bookrecommend WHERE userId = ? "
                + "ORDER BY createTime DESC, id DESC";
        return JDBCTool.queryList(connection, sql, MAPPER, userId);
    }

    public List<RecommendVO> findPending(Connection connection) throws Exception {
        String sql = "SELECT r.id, r.createTime, r.updateTime, r.userId, r.bookName, r.author, "
                + "r.publisher, r.price, r.status, r.reason, u.name AS userName "
                + "FROM bookrecommend r JOIN user u ON u.id = r.userId "
                + "WHERE r.status = '待审核' ORDER BY r.createTime ASC, r.id ASC";
        return JDBCTool.queryList(connection, sql, rs -> {
            BookRecommend recommend = mapRecommend(rs);
            return RecommendVO.from(recommend, rs.getString("userName"));
        });
    }

    public BookRecommend findByIdForUpdate(String id, Connection connection) throws Exception {
        return JDBCTool.queryOne(connection,
                "SELECT " + COLUMNS + " FROM bookrecommend WHERE id = ? FOR UPDATE",
                MAPPER, id);
    }

    public void markApproved(String id, LocalDateTime now, Connection connection) throws Exception {
        JDBCTool.executeUpdate(connection,
                "UPDATE bookrecommend SET status = '已通过', updateTime = ? WHERE id = ? AND status = '待审核'",
                now, id);
    }

    public void markRejected(String id, String reason, LocalDateTime now, Connection connection) throws Exception {
        JDBCTool.executeUpdate(connection,
                "UPDATE bookrecommend SET status = '已驳回', reason = ?, updateTime = ? "
                        + "WHERE id = ? AND status = '待审核'",
                reason, now, id);
    }

    private static BookRecommend mapRecommend(ResultSet rs) throws java.sql.SQLException {
        BookRecommend recommend = new BookRecommend();
        recommend.setId(rs.getString("id"));
        recommend.setCreateTime(toDateTime(rs.getTimestamp("createTime")));
        recommend.setUpdateTime(toDateTime(rs.getTimestamp("updateTime")));
        recommend.setUserId(rs.getString("userId"));
        recommend.setBookName(rs.getString("bookName"));
        recommend.setAuthor(rs.getString("author"));
        recommend.setPublisher(rs.getString("publisher"));
        recommend.setPrice(rs.getObject("price") == null ? null : rs.getDouble("price"));
        recommend.setStatus(rs.getString("status"));
        recommend.setReason(rs.getString("reason"));
        return recommend;
    }

    private static LocalDateTime toDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
