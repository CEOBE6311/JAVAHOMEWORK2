package com.library.service;

import com.library.dao.LoanDao;
import com.library.dao.UserDao;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.dto.UserVO;
import com.library.entity.Admin;
import com.library.entity.Operator;
import com.library.entity.Student;
import com.library.entity.Teacher;
import com.library.entity.User;
import com.library.jdbc.JDBCTool;
import com.library.util.ApiException;
import com.library.util.DateUtil;
import com.library.util.DbConfig;
import com.library.util.IdUtil;
import com.library.util.SessionUser;
import com.library.util.ValidateUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthService {

    private static final Object REGISTER_LOCK = new Object();

    private final UserDao userDao = new UserDao();
    private final LoanDao loanDao = new LoanDao();

    public Map<String, Object> login(LoginRequest request, HttpServletRequest httpRequest) throws Exception {
        if (request == null) {
            throw new ApiException(400, 1001, "请求体不能为空");
        }
        String name = ValidateUtil.text(request.name, "用户名", 1, 50);
        String password = request.password == null ? "" : request.password;
        if (password.isEmpty()) {
            throw new ApiException(400, 1001, "密码不能为空");
        }
        checkCaptcha(request.captcha, httpRequest.getSession(true));

        LocalDateTime now = DateUtil.now();
        User user = JDBCTool.transaction(connection -> {
            User found = userDao.findByNameAndPassword(name, password, connection);
            if (found == null) {
                throw new ApiException(401, 2001, "用户名或密码错误");
            }
            userDao.updateLoginTime(found.getId(), now, connection);
            found.setLoginTime(now);
            loanDao.refreshOverdue(now, connection);
            return found;
        });

        SessionUser sessionUser = new SessionUser(user.getId(), user.getName(), user.getRole(), user.hasPermission());
        httpRequest.changeSessionId();
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("loginUser", sessionUser);
        session.setAttribute("loginTime", now);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", UserVO.from(user));
        result.put("permissions", user.hasPermission());
        return result;
    }

    public void checkCaptcha(String input, HttpSession session) {
        if (!DbConfig.captchaEnabled()) {
            return;
        }
        Object expected = session.getAttribute("captcha");
        Object time = session.getAttribute("captchaTime");
        session.removeAttribute("captcha");
        session.removeAttribute("captchaTime");
        if (!(expected instanceof String code) || !(time instanceof Long createdAt)
                || System.currentTimeMillis() - createdAt > 5 * 60 * 1000L) {
            throw new ApiException(400, 1001, "验证码已过期，请刷新");
        }
        if (input == null || !code.equalsIgnoreCase(input.trim())) {
            throw new ApiException(400, 1001, "验证码错误");
        }
    }

    public UserVO register(RegisterRequest request) throws Exception {
        if (request == null) {
            throw new ApiException(400, 1001, "请求体不能为空");
        }
        String name = ValidateUtil.text(request.name, "姓名", 2, 20);
        String password = ValidateUtil.text(request.password, "密码", 6, 32);
        String phone = ValidateUtil.phone(request.phone);
        String major = ValidateUtil.text(request.major, "专业", 1, 50);
        String role = ValidateUtil.role(request.role);

        if (!DbConfig.allowStaffRegister() && ("管理员".equals(role) || "操作员".equals(role))) {
            throw new ApiException(403, 2002, "当前不允许注册管理员/操作员");
        }

        User user = buildUser(request, role);
        user.setId(IdUtil.uuid7());
        LocalDateTime now = DateUtil.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setName(name);
        user.setPassword(password);
        user.setPhone(phone);
        user.setRole(role);
        user.setMajor(major);
        user.setLoginTime(null);

        synchronized (REGISTER_LOCK) {
            JDBCTool.transaction(connection -> {
                if (userDao.existsName(name, connection)) {
                    throw new ApiException(409, 3002, "该用户名已被注册");
                }
                if (userDao.existsPhone(phone, connection)) {
                    throw new ApiException(409, 3002, "该手机号已被注册");
                }
                userDao.insert(user, connection);
                return null;
            });
        }
        return UserVO.from(user);
    }

    private User buildUser(RegisterRequest request, String role) {
        if ("管理员".equals(role)) {
            Admin admin = new Admin();
            admin.setPermissionLevel(ValidateUtil.intRange(request.permissionLevel, "权限等级", 1, 3));
            admin.setEntryTime(DateUtil.parseDate(request.entryTime, "入职时间"));
            if (admin.getEntryTime() == null) {
                throw new ApiException(400, 1001, "入职时间不能为空");
            }
            return admin;
        }
        if ("操作员".equals(role)) {
            Operator operator = new Operator();
            operator.setArea(ValidateUtil.text(request.area, "负责区域", 1, 50));
            operator.setDuty(ValidateUtil.text(request.duty, "职务", 1, 50));
            return operator;
        }
        if ("学生".equals(role)) {
            Student student = new Student();
            student.setGrade(ValidateUtil.text(request.grade, "年级", 1, 20));
            student.setCollege(ValidateUtil.text(request.college, "所属学院", 1, 50));
            return student;
        }
        Teacher teacher = new Teacher();
        teacher.setCollege(ValidateUtil.text(request.college, "所属学院", 1, 50));
        teacher.setTitle(ValidateUtil.text(request.title, "职称", 1, 20));
        return teacher;
    }

    public UserVO me(SessionUser sessionUser) throws Exception {
        if (sessionUser == null) {
            throw new ApiException(401, 2001, "请先登录");
        }
        try (var connection = JDBCTool.getConnection()) {
            User user = userDao.findById(sessionUser.id, connection);
            if (user == null) {
                throw new ApiException(401, 2001, "用户不存在，请重新登录");
            }
            return UserVO.from(user);
        }
    }
}
