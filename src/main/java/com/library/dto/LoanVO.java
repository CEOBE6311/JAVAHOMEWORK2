package com.library.dto;

import com.library.entity.Loan;

import java.time.LocalDateTime;

public class LoanVO {

    public String id;
    public String copyId;
    public String bookId;
    public String bookTitle;
    public String userId;
    public String userName;
    public LocalDateTime borrowDate;
    public LocalDateTime dueDate;
    public LocalDateTime returnDate;
    public String status;
    public boolean overdue;

    public static LoanVO from(Loan loan, String userName, String bookId, String bookTitle) {
        if (loan == null) {
            return null;
        }
        LoanVO vo = new LoanVO();
        vo.id = loan.getId();
        vo.copyId = loan.getBookCopyId();
        vo.bookId = bookId;
        vo.bookTitle = bookTitle;
        vo.userId = loan.getUserId();
        vo.userName = userName;
        vo.borrowDate = loan.getBorrowDate();
        vo.dueDate = loan.getDueDate();
        vo.returnDate = loan.getReturnDate();
        vo.status = loan.getStatus();
        vo.overdue = loan.getReturnDate() == null
                && loan.getDueDate() != null
                && loan.getDueDate().isBefore(LocalDateTime.now());
        return vo;
    }
}
