package com.zjgsu.zyw.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zjgsu.zyw.constant.BookStatusConstant;
import com.zjgsu.zyw.dto.BookPageDTO;
import com.zjgsu.zyw.enity.Book;
import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.mapper.BookMapper;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookMapper bookMapper;

    @Override
    public PageResult pageSearch(BookPageDTO bookPageDTO) {
        PageHelper.startPage(bookPageDTO.getPage(),bookPageDTO.getPageSize());
        List<Book> list =  bookMapper.getBook(bookPageDTO);
        Page<Book> p =(Page<Book>)list;
        return new PageResult(p.getTotal(),p.getResult());
    }

    @Override
    public void createBook(Book book) {
        bookMapper.create(book);
    }

    @Override
    public void delete(Integer id) {
        Book book = bookMapper.list(id);
        if (book == null) {
            throw new BaseException("图书不存在");
        }
        if(BookStatusConstant.BORROWED.equals(book.getStatus())){
            throw new BaseException("图书正处于出借状态，不可删除");
        }
        bookMapper.deleteById(id);
    }

    @Override
    public Book getBookById(Integer id) {
        Book book = bookMapper.list(id);
        if (book == null) {
            throw new BaseException("图书不存在");
        }
        return book;
    }

    @Override
    public void updateBook(Book book) {
        bookMapper.updateById(book);
    }

    @Override
    public void updateStatus(Integer id, String status) {
        bookMapper.setStatusById(id, status);
    }
}
