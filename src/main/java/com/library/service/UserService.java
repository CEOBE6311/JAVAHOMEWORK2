package com.library.service;

import com.library.dao.UserDao;
import com.library.dto.UserVO;
import com.library.entity.User;
import com.library.jdbc.JDBCTool;
import com.library.util.ApiException;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final UserDao userDao = new UserDao();

    public List<UserVO> search(String keyword) throws Exception {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        try (Connection connection = JDBCTool.getConnection()) {
            List<UserVO> result = new ArrayList<>();
            for (User user : userDao.search(keyword, connection)) {
                result.add(UserVO.from(user));
            }
            return result;
        }
    }

    public UserVO detail(String id) throws Exception {
        try (Connection connection = JDBCTool.getConnection()) {
            User user = userDao.findById(id, connection);
            if (user == null) {
                throw new ApiException(404, 3001, "用户不存在");
            }
            return UserVO.from(user);
        }
    }
}
