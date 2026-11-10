package com.library.entity;

import com.library.jdbc.annotation.Column;
import com.library.jdbc.annotation.Table;

import java.time.LocalDateTime;

@Table("loan")
public class Loan extends Base {

    @Column("bookCopyId")
    private String bookCopyId;

    @Column("userId")
    private String userId;

    private transient BookCopy bookCopy;

    private transient User user;

    @Column("borrowDate")
    private LocalDateTime borrowDate;

    @Column("returnDate")
    private LocalDateTime returnDate;

    @Column("dueDate")
    private LocalDateTime dueDate;

    /** 未还 / 已还 / 超期 */
    @Column("status")
    private String status;

    public String getBookCopyId() {
        return bookCopyId;
    }

    public void setBookCopyId(String bookCopyId) {
        this.bookCopyId = bookCopyId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BookCopy getBookCopy() {
        return bookCopy;
    }

    public void setBookCopy(BookCopy bookCopy) {
        this.bookCopy = bookCopy;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
