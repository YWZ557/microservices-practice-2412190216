package com.zjgsu.zyw.controller.user;

import com.zjgsu.zyw.dto.BookPageDTO;
import com.zjgsu.zyw.enity.Book;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("readerBookController")
@RequestMapping("/api/reader/book")
public class BookController {
    @Autowired
    private BookService bookService;

    @GetMapping("/{id}")
    public Result getBookById(@PathVariable("id") Integer id){
        Book book = bookService.getBookById(id);
        return Result.success(book);
    }
    @PostMapping("/page")
    public Result page(@RequestBody BookPageDTO bookPageDTO){
        PageResult pageResult = bookService.pageSearch(bookPageDTO);
        return Result.success(pageResult);
    }




}
