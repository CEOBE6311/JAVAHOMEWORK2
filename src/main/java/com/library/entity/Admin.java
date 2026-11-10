package com.library.entity;

import com.library.jdbc.annotation.Column;
import com.library.jdbc.annotation.Table;
import com.library.util.Permission;

import java.time.LocalDate;

@Table("user")
public class Admin extends User {

    @Column("permissionLevel")
    private Integer permissionLevel;

    @Column("entryTime")
    private LocalDate entryTime;

    public Integer getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(Integer permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public LocalDate getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDate entryTime) {
        this.entryTime = entryTime;
    }

    @Override
    public String[] hasPermission() {
        return new String[]{
                Permission.USER_QUERY, Permission.LOAN_QUERY, Permission.LOAN_STATS,
                Permission.LOAN_BORROW, Permission.LOAN_RETURN, Permission.LOAN_SELF,
                Permission.RECOMMEND_REVIEW
        };
    }
}
