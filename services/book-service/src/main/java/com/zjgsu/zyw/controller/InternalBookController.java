package com.zjgsu.zyw.controller;

import com.zjgsu.zyw.constant.BookStatusConstant;
import com.zjgsu.zyw.enity.Book;
import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.message.BorrowMessage;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供其它微服务调用的图书内部接口。
 * 路径以 /internal 开头，不经过 JWT 拦截器。
 */
@RestController
@RequestMapping("/internal/books")
public class InternalBookController {

    @Autowired
    private BookService bookService;

    @GetMapping("/{id}")
    public Result getById(@PathVariable("id") Integer id) {
        try {
            return Result.success(bookService.getBookById(id));
        } catch (BaseException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 借出图书：仅允许 AVAILABLE -> BORROWED。
     */
    @PostMapping("/{id}/borrow")
    public Result borrow(@PathVariable("id") Integer id) {
        Book book;
        try {
            book = bookService.getBookById(id);
        } catch (BaseException e) {
            return Result.error(e.getMessage());
        }
        if (BookStatusConstant.REMOVED.equals(book.getStatus())) {
            return Result.error(BorrowMessage.BOOK_REMOVED);
        }
        if (BookStatusConstant.BORROWED.equals(book.getStatus())) {
            return Result.error(BorrowMessage.BOOK_NO_STOCK);
        }
        bookService.updateStatus(id, BookStatusConstant.BORROWED);
        return Result.success();
    }

    /**
     * 归还图书：BORROWED/OVERDUE -> AVAILABLE。
     */
    @PostMapping("/{id}/return")
    public Result returnBook(@PathVariable("id") Integer id) {
        try {
            bookService.getBookById(id);
        } catch (BaseException e) {
            return Result.error(e.getMessage());
        }
        bookService.updateStatus(id, BookStatusConstant.AVAILABLE);
        return Result.success();
    }
}
