package com.zjgsu.zyw.controller;


import com.zjgsu.zyw.pojo.dto.BorrowRecordDTO;
import com.zjgsu.zyw.pojo.enity.BorrowRecord;
import com.zjgsu.zyw.pojo.enity.Result;
import com.zjgsu.zyw.service.BorrowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/borrow")
public class BorrowController {
    @Autowired
    private BorrowService borrowService;

    @GetMapping("/{id}")
    public Result getBorrowRecordByReaderId(@PathVariable("id") Integer id){
        List<BorrowRecord> records = borrowService.getBorrowRecordById(id);
        if(records.isEmpty()){
            log.info("查询失败，内容为空");
            return Result.error("无相关数据");
        }
        log.info("查询成功");
        return Result.success(records);
    }
    @PostMapping
    public Result CreateRecord(@RequestBody BorrowRecordDTO borrowRecordDTO){
        log.info("创建借书记录{}",borrowRecordDTO);
        borrowService.CreateRecord(borrowRecordDTO);
        return Result.success();
    }


}
