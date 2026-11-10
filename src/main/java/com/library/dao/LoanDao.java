package com.library.dao;

import com.library.dto.LoanVO;
import com.library.dto.StatItem;
import com.library.entity.Loan;
import com.library.jdbc.JDBCTool;
import com.library.jdbc.RowMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoanDao {

    private static final String VO_SELECT = "SELECT l.id, l.bookCopyId, l.userId, l.borrowDate, "
            + "l.returnDate, l.dueDate, l.status, u.name AS userName, b.id AS bookId, b.title AS bookTitle "
            + "FROM loan l "
            + "JOIN user u ON u.id = l.userId "
            + "JOIN bookcopy c ON c.id = l.bookCopyId "
            + "JOIN book b ON b.id = c.bookId ";

    private static final RowMapper<LoanVO> VO_MAPPER = LoanDao::mapVO;

    public void insert(Loan loan, Connection connection) throws Exception {
        JDBCTool.save(loan, connection);
    }

    public Loan findActiveByCopyAndUserForUpdate(String copyId, String userId, Connection connection) throws Exception {
        String sql = "SELECT id, bookCopyId, userId, borrowDate, returnDate, dueDate, status "
                + "FROM loan WHERE bookCopyId = ? AND userId = ? AND returnDate IS NULL "
                + "ORDER BY borrowDate DESC LIMIT 1 FOR UPDATE";
        return JDBCTool.queryOne(connection, sql, LoanDao::mapLoan, copyId, userId);
    }

    public void markReturned(String loanId, LocalDateTime now, Connection connection) throws Exception {
        JDBCTool.executeUpdate(connection,
                "UPDATE loan SET returnDate = ?, status = '已还', updateTime = ? WHERE id = ?",
                now, now, loanId);
    }

    public List<LoanVO> findByUser(String userId, boolean unreturned,
                                   LocalDateTime start, LocalDateTime end, Connection connection) throws Exception {
        StringBuilder sql = new StringBuilder(VO_SELECT).append(" WHERE l.userId = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (unreturned) {
            sql.append(" AND l.returnDate IS NULL");
        }
        if (start != null) {
            sql.append(" AND l.borrowDate >= ?");
            args.add(start);
        }
        if (end != null) {
            sql.append(" AND l.borrowDate <= ?");
            args.add(end);
        }
        sql.append(" ORDER BY l.borrowDate DESC, l.id DESC");
        return JDBCTool.queryList(connection, sql.toString(), VO_MAPPER, args.toArray());
    }

    public List<LoanVO> search(String mode, String keyword,
                               LocalDateTime start, LocalDateTime end, Connection connection) throws Exception {
        StringBuilder sql = new StringBuilder(VO_SELECT).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();

        if ("overdue".equals(mode)) {
            sql.append(" AND l.returnDate IS NULL AND l.dueDate < ?");
            args.add(LocalDateTime.now());
        } else if ("range".equals(mode)) {
            if (start == null || end == null) {
                throw new IllegalArgumentException("日期范围不能为空");
            }
            sql.append(" AND l.borrowDate >= ? AND l.borrowDate <= ?");
            args.add(start);
            args.add(end);
        }

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + escapeLike(keyword.trim()) + "%";
            sql.append(" AND (u.name LIKE ? OR b.title LIKE ?)");
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY l.borrowDate DESC, l.id DESC");
        return JDBCTool.queryList(connection, sql.toString(), VO_MAPPER, args.toArray());
    }

    public int refreshOverdue(LocalDateTime now, Connection connection) throws Exception {
        return JDBCTool.executeUpdate(connection,
                "UPDATE loan SET status = '超期', updateTime = ? "
                        + "WHERE status = '未还' AND returnDate IS NULL AND dueDate < ?",
                now, now);
    }

    public List<StatItem> statsByType(Connection connection) throws Exception {
        String sql = "SELECT b.type AS name, COUNT(l.id) AS total "
                + "FROM book b "
                + "LEFT JOIN bookcopy c ON c.bookId = b.id "
                + "LEFT JOIN loan l ON l.bookCopyId = c.id "
                + "GROUP BY b.type ORDER BY total DESC, b.type ASC";
        return JDBCTool.queryList(connection, sql,
                rs -> new StatItem(rs.getString("name"), rs.getLong("total")));
    }

    public List<StatItem> statsByMajor(Connection connection) throws Exception {
        String sql = "SELECT COALESCE(NULLIF(u.major, ''), '未填写') AS name, COUNT(l.id) AS total "
                + "FROM user u LEFT JOIN loan l ON l.userId = u.id "
                + "GROUP BY COALESCE(NULLIF(u.major, ''), '未填写') "
                + "ORDER BY total DESC, name ASC";
        return JDBCTool.queryList(connection, sql,
                rs -> new StatItem(rs.getString("name"), rs.getLong("total")));
    }

    private static Loan mapLoan(ResultSet rs) throws java.sql.SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getString("id"));
        loan.setBookCopyId(rs.getString("bookCopyId"));
        loan.setUserId(rs.getString("userId"));
        loan.setBorrowDate(toDateTime(rs.getTimestamp("borrowDate")));
        loan.setReturnDate(toDateTime(rs.getTimestamp("returnDate")));
        loan.setDueDate(toDateTime(rs.getTimestamp("dueDate")));
        loan.setStatus(rs.getString("status"));
        return loan;
    }

    private static LoanVO mapVO(ResultSet rs) throws java.sql.SQLException {
        Loan loan = mapLoan(rs);
        return LoanVO.from(loan, rs.getString("userName"), rs.getString("bookId"), rs.getString("bookTitle"));
    }

    private static LocalDateTime toDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
