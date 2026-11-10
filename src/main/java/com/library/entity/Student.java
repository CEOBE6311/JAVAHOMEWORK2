package com.library.entity;

import com.library.jdbc.annotation.Column;
import com.library.jdbc.annotation.Table;
import com.library.util.Permission;

@Table("user")
public class Student extends User {

    @Column("grade")
    private String grade;

    @Column("college")
    private String college;

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    @Override
    public String[] hasPermission() {
        return new String[]{
                Permission.LOAN_BORROW, Permission.LOAN_RETURN, Permission.LOAN_SELF
        };
    }
}
