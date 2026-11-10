package com.library.entity;

import com.library.jdbc.annotation.Column;
import com.library.jdbc.annotation.Table;
import com.library.util.Permission;

@Table("user")
public class Operator extends User {

    @Column("area")
    private String area;

    @Column("duty")
    private String duty;

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getDuty() {
        return duty;
    }

    public void setDuty(String duty) {
        this.duty = duty;
    }

    @Override
    public String[] hasPermission() {
        return new String[]{
                Permission.BOOK_READ, Permission.BOOK_ADD, Permission.BOOK_EDIT,
                Permission.BOOK_DISCARD, Permission.COPY_EDIT,
                Permission.LOAN_BORROW, Permission.LOAN_RETURN, Permission.LOAN_SELF
        };
    }
}
