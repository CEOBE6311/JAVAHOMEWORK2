package com.library.dto;

import com.library.entity.BookRecommend;

import java.time.LocalDateTime;

public class RecommendVO {

    public String id;
    public String userId;
    public String userName;
    public String bookName;
    public String author;
    public String publisher;
    public Double price;
    public String status;
    public String reason;
    public LocalDateTime createTime;

    public static RecommendVO from(BookRecommend recommend, String userName) {
        if (recommend == null) {
            return null;
        }
        RecommendVO vo = new RecommendVO();
        vo.id = recommend.getId();
        vo.userId = recommend.getUserId();
        vo.userName = userName;
        vo.bookName = recommend.getBookName();
        vo.author = recommend.getAuthor();
        vo.publisher = recommend.getPublisher();
        vo.price = recommend.getPrice();
        vo.status = recommend.getStatus();
        vo.reason = recommend.getReason();
        vo.createTime = recommend.getCreateTime();
        return vo;
    }
}
