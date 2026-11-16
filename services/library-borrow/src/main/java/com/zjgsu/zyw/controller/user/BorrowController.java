package com.zjgsu.zyw.controller.user;


import com.zjgsu.zyw.context.BaseContext;
import com.zjgsu.zyw.dto.BorrowPageDTO;
import com.zjgsu.zyw.dto.BorrowRecordDTO;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.BorrowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("readerBorrowController")
@RequestMapping("/borrow/reader")
public class BorrowController {
    @Autowired
    private BorrowService borrowService;

    @PostMapping("/page")
    public Result pageSearch(@RequestBody BorrowPageDTO borrowPageDTO){
        borrowPageDTO.setReaderId(BaseContext.getCurrentId());
        borrowPageDTO.setName(null);
        PageResult page = borrowService.pageSearch(borrowPageDTO);
        return Result.success(page);
    }
    @PostMapping
    public Result createRecord(@RequestBody BorrowRecordDTO borrowRecordDTO){
        borrowRecordDTO.setReaderId(BaseContext.getCurrentId());
        log.info("创建借书记录{}",borrowRecordDTO);
        borrowService.CreateRecord(borrowRecordDTO);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    public Result deleteRecordById(@PathVariable("id") Integer id){
        log.info("删除id{}的记录",id);
        borrowService.DeleteRecordById(id);
        return Result.success();
    }

    @GetMapping("/return/{id}")
    public Result returnBook(@PathVariable("id") Integer id){
        log.info("归还id{}书",id);
        borrowService.ReturnBook(id);
        return Result.success();
    }

}
