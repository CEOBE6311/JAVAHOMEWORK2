package com.library.dto;

import com.library.entity.Book;

import java.time.LocalDateTime;

public class BookVO {

    public String id;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    public String title;
    public String author;
    public String type;
    public String publisher;
    public Integer days;
    public Double price;
    public Integer stock;
    public String status;
    public String introduction;

    public int totalCopies;
    public int borrowedCopies;

    public static BookVO from(Book book, int totalCopies, int borrowedCopies) {
        if (book == null) {
            return null;
        }
        BookVO vo = new BookVO();
        vo.id = book.getId();
        vo.createTime = book.getCreateTime();
        vo.updateTime = book.getUpdateTime();
        vo.title = book.getTitle();
        vo.author = book.getAuthor();
        vo.type = book.getType();
        vo.publisher = book.getPublisher();
        vo.days = book.getDays();
        vo.price = book.getPrice();
        vo.stock = book.getStock();
        vo.status = book.getStatus();
        vo.introduction = book.getIntroduction();
        vo.totalCopies = totalCopies;
        vo.borrowedCopies = borrowedCopies;
        return vo;
    }
}
