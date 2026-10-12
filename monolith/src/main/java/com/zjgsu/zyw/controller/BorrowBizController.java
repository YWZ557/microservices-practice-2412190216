package com.zjgsu.zyw.controller;

import com.zjgsu.zyw.dto.BorrowRequest;
import com.zjgsu.zyw.pojo.BorrowRecord;
import com.zjgsu.zyw.pojo.Result;
import com.zjgsu.zyw.service.BorrowBizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基于业务规则（BorrowBizService）的借书接口。
 * 使用独立路径 /api/biz，避免与既有 MyBatis / JPA 接口冲突。
 */
@RestController
@RequestMapping("/api/biz")
public class BorrowBizController {

    @Autowired
    private BorrowBizService borrowBizService;

    /**
     * 借书：成功返回成功结果与借阅记录，业务规则不通过返回错误结果。
     */
    @PostMapping("/borrow")
    public Result borrow(@RequestBody BorrowRequest request) {
        try {
            BorrowRecord record = borrowBizService.borrow(request);
            return Result.success(record);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }
}
