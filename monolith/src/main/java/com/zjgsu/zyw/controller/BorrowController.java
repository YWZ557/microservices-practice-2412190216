package com.zjgsu.zyw.controller;


import com.zjgsu.zyw.pojo.BorrowRecord;
import com.zjgsu.zyw.pojo.Result;
import com.zjgsu.zyw.service.BorrowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class BorrowController {
    @Autowired
    private BorrowService borrowService;

    @GetMapping("/{id}")
    public Result QueryRecord(@PathVariable("id") Integer id){
        List<BorrowRecord> records = borrowService.QueryRecordById(id);
        if(records.isEmpty()){
            log.info("查询失败，内容为空");
            return Result.error("NoPointer");
        }
        log.info("查询成功");
        return Result.success(records);
    }
    @PostMapping
    public Result CreateRecord(@RequestBody BorrowRecord borrowRecord){
        Integer rows = borrowService.CreateRecord(borrowRecord);
        if (rows == 0){
            log.info("创建失败");
            return Result.error("创建失败");
        }
        return Result.success(rows);
    }


}
