package com.library.entity;

import com.library.jdbc.annotation.Column;
import com.library.jdbc.annotation.Table;
import com.library.util.Permission;

@Table("user")
public class Teacher extends User {

    @Column("college")
    private String college;

    @Column("title")
    private String title;

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String[] hasPermission() {
        return new String[]{
                Permission.LOAN_BORROW, Permission.LOAN_RETURN, Permission.LOAN_SELF,
                Permission.RECOMMEND_ADD
        };
    }
}
