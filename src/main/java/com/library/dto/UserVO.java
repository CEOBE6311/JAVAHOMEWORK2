package com.library.dto;

import com.library.entity.Admin;
import com.library.entity.Operator;
import com.library.entity.Student;
import com.library.entity.Teacher;
import com.library.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 用户展示对象：没有 password 字段。 */
public class UserVO {

    public String id;
    public String name;
    public String role;
    public String phone;
    public String major;
    public LocalDateTime createTime;
    public LocalDateTime loginTime;

    public Integer permissionLevel;
    public LocalDate entryTime;
    public String area;
    public String duty;
    public String grade;
    public String college;
    public String title;

    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.id = user.getId();
        vo.name = user.getName();
        vo.role = user.getRole();
        vo.phone = user.getPhone();
        vo.major = user.getMajor();
        vo.createTime = user.getCreateTime();
        vo.loginTime = user.getLoginTime();

        if (user instanceof Admin admin) {
            vo.permissionLevel = admin.getPermissionLevel();
            vo.entryTime = admin.getEntryTime();
        } else if (user instanceof Operator operator) {
            vo.area = operator.getArea();
            vo.duty = operator.getDuty();
        } else if (user instanceof Student student) {
            vo.grade = student.getGrade();
            vo.college = student.getCollege();
        } else if (user instanceof Teacher teacher) {
            vo.college = teacher.getCollege();
            vo.title = teacher.getTitle();
        }
        return vo;
    }
}
