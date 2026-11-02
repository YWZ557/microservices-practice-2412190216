package com.zjgsu.zyw.controller.admin;

import com.zjgsu.zyw.dto.BookPageDTO;
import com.zjgsu.zyw.enity.Book;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController("adminBookController")
@RequestMapping("/book/admin")
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
    @PostMapping("/craete")
    public Result createBook(@RequestBody Book book){
        bookService.createBook(book);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    public Result deleteBook(@PathVariable("id") Integer id){
        bookService.delete(id);
        return Result.success();
    }
    @PostMapping("/update")
    public Result updateBook(@RequestBody Book book){
        bookService.updateBook(book);
        return Result.success();
    }



}
