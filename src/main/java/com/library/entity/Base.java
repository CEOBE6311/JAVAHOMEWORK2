package com.library.entity;

import com.library.jdbc.annotation.Column;
import com.library.jdbc.annotation.Id;

import java.time.LocalDateTime;

/** 所有实体的公共字段。 */
public class Base {

    @Id
    @Column("id")
    private String id;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
