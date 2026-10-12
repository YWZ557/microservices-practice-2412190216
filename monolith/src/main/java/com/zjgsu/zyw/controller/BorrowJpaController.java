package com.zjgsu.zyw.controller;

import com.zjgsu.zyw.dto.BorrowQueryRequest;
import com.zjgsu.zyw.pojo.Book;
import com.zjgsu.zyw.pojo.BorrowRecord;
import com.zjgsu.zyw.pojo.Result;
import com.zjgsu.zyw.repository.BookRepository;
import com.zjgsu.zyw.repository.BorrowRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 基于 Spring Data JPA 的示例控制器。
 * 使用独立的 /api/jpa 路径，避免与原有 MyBatis 接口冲突。
 */
@Slf4j
@RestController
@RequestMapping("/api/jpa")
public class BorrowJpaController {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private BookRepository bookRepository;

    /**
     * 使用 DTO 承载输入，按读者 ID 查询借阅记录。
     */
    @PostMapping("/records")
    public Result queryByReader(@RequestBody BorrowQueryRequest request) {
        List<BorrowRecord> records = borrowRecordRepository.findByReaderId(request.getReaderId());
        if (records.isEmpty()) {
            return Result.error("NoPointer");
        }
        return Result.success(records);
    }

    /**
     * 列出全部图书。
     */
    @GetMapping("/books")
    public Result listBooks() {
        List<Book> books = bookRepository.findAll();
        return Result.success(books);
    }
}
