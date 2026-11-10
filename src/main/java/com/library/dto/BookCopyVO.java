package com.library.dto;

import com.library.entity.BookCopy;

import java.time.LocalDateTime;

public class BookCopyVO {

    public String id;
    public String bookId;
    public String bookTitle;
    public boolean isBorrowed;
    public LocalDateTime createTime;

    public static BookCopyVO from(BookCopy copy, String bookTitle) {
        if (copy == null) {
            return null;
        }
        BookCopyVO vo = new BookCopyVO();
        vo.id = copy.getId();
        vo.bookId = copy.getBookId();
        vo.bookTitle = bookTitle;
        vo.isBorrowed = copy.isBorrowed();
        vo.createTime = copy.getCreateTime();
        return vo;
    }
}
