package com.library.service;

import com.library.dao.LoanDao;
import com.library.dto.StatItem;
import com.library.jdbc.JDBCTool;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class StatsService {

    private final LoanDao loanDao = new LoanDao();

    public List<StatItem> byType() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = JDBCTool.getConnection()) {
            loanDao.refreshOverdue(now, connection);
            return loanDao.statsByType(connection);
        }
    }

    public List<StatItem> byMajor() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = JDBCTool.getConnection()) {
            loanDao.refreshOverdue(now, connection);
            return loanDao.statsByMajor(connection);
        }
    }
}
