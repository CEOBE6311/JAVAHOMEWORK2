package com.library.entity;

import com.library.jdbc.annotation.Column;
import com.library.jdbc.annotation.Table;

@Table("bookcopy")
public class BookCopy extends Base {

    /** 适配 JDBCTool 的外键列。 */
    @Column("bookId")
    private String bookId;

    private transient Book book;

    /** true=已借出，false=在馆。 */
    @Column("isBorrowed")
    private boolean isBorrowed;

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public boolean isBorrowed() {
        return isBorrowed;
    }

    public void setBorrowed(boolean borrowed) {
        isBorrowed = borrowed;
    }
}
