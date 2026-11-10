package com.library.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/** 结果集行映射。 */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
}
