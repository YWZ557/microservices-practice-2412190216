package com.zjgsu.zyw.service;

import com.zjgsu.zyw.dto.BookPageDTO;
import com.zjgsu.zyw.enity.Book;
import com.zjgsu.zyw.result.PageResult;

public interface BookService {
    PageResult pageSearch(BookPageDTO bookPageDTO);

    void createBook(Book book);

    void delete(Integer id);

    Book getBookById(Integer id);

    void updateBook(Book book);

    /**
     * 更新图书状态，供借阅服务内部调用。
     */
    void updateStatus(Integer id, String status);
}
