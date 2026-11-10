package com.library.dao;

import com.library.entity.Admin;
import com.library.entity.Operator;
import com.library.entity.Student;
import com.library.entity.Teacher;
import com.library.entity.User;
import com.library.jdbc.JDBCTool;
import com.library.jdbc.RowMapper;
import com.library.util.ApiException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

public class UserDao {

    private static final String COLUMNS = "id, createTime, updateTime, name, password, phone, role, major, "
            + "loginTime, permissionLevel, entryTime, duty, area, grade, college, title";

    private static final RowMapper<User> MAPPER = UserDao::mapUser;

    public User findByNameAndPassword(String name, String password, Connection connection) throws Exception {
        String sql = "SELECT " + COLUMNS + " FROM user WHERE name = ? AND password = ? LIMIT 1";
        return JDBCTool.queryOne(connection, sql, MAPPER, name, password);
    }

    public User findByName(String name, Connection connection) throws Exception {
        String sql = "SELECT " + COLUMNS + " FROM user WHERE name = ? LIMIT 1";
        return JDBCTool.queryOne(connection, sql, MAPPER, name);
    }

    public User findById(String id, Connection connection) throws Exception {
        String sql = "SELECT " + COLUMNS + " FROM user WHERE id = ? LIMIT 1";
        return JDBCTool.queryOne(connection, sql, MAPPER, id);
    }

    public boolean existsName(String name, Connection connection) throws Exception {
        return JDBCTool.queryOne(connection, "SELECT 1 FROM user WHERE name = ? LIMIT 1",
                rs -> rs.getInt(1), name) != null;
    }

    public boolean existsPhone(String phone, Connection connection) throws Exception {
        return JDBCTool.queryOne(connection, "SELECT 1 FROM user WHERE phone = ? LIMIT 1",
                rs -> rs.getInt(1), phone) != null;
    }

    public List<User> search(String keyword, Connection connection) throws Exception {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String like = "%" + escapeLike(keyword.trim()) + "%";
        String sql = "SELECT " + COLUMNS + " FROM user "
                + "WHERE name LIKE ? OR phone LIKE ? OR role LIKE ? OR major LIKE ? "
                + "ORDER BY createTime DESC, id DESC";
        return JDBCTool.queryList(connection, sql, MAPPER, like, like, like, like);
    }

    public void insert(User user, Connection connection) throws Exception {
        JDBCTool.save(user, connection);
    }

    public void updateLoginTime(String id, LocalDateTime loginTime, Connection connection) throws Exception {
        JDBCTool.executeUpdate(connection,
                "UPDATE user SET loginTime = ?, updateTime = ? WHERE id = ?",
                loginTime, loginTime, id);
    }

    public static User mapUser(ResultSet rs) throws java.sql.SQLException {
        String role = rs.getString("role");
        User user;
        if ("管理员".equals(role)) {
            Admin admin = new Admin();
            admin.setPermissionLevel((Integer) rs.getObject("permissionLevel"));
            java.sql.Date entry = rs.getDate("entryTime");
            admin.setEntryTime(entry == null ? null : entry.toLocalDate());
            user = admin;
        } else if ("操作员".equals(role)) {
            Operator operator = new Operator();
            operator.setArea(rs.getString("area"));
            operator.setDuty(rs.getString("duty"));
            user = operator;
        } else if ("学生".equals(role)) {
            Student student = new Student();
            student.setGrade(rs.getString("grade"));
            student.setCollege(rs.getString("college"));
            user = student;
        } else if ("教师".equals(role)) {
            Teacher teacher = new Teacher();
            teacher.setCollege(rs.getString("college"));
            teacher.setTitle(rs.getString("title"));
            user = teacher;
        } else {
            throw new ApiException(500, 5001, "未知角色: " + role);
        }

        user.setId(rs.getString("id"));
        user.setCreateTime(toDateTime(rs.getTimestamp("createTime")));
        user.setUpdateTime(toDateTime(rs.getTimestamp("updateTime")));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));
        user.setPhone(rs.getString("phone"));
        user.setRole(role);
        user.setMajor(rs.getString("major"));
        user.setLoginTime(toDateTime(rs.getTimestamp("loginTime")));
        return user;
    }

    private static LocalDateTime toDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
