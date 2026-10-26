package com.zjgsu.zyw.controller.admin;



import com.zjgsu.zyw.dto.BorrowPageDTO;
import com.zjgsu.zyw.dto.BorrowRecordDTO;
import com.zjgsu.zyw.enity.BorrowRecord;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.BorrowService;
import com.zjgsu.zyw.vo.BorrowRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("adminBorrowController")
@RequestMapping("/api/admin/borrow")
public class BorrowController {
    @Autowired
    private BorrowService borrowService;

    @GetMapping("/list")
    public Result getBorrowRecord(){
        List<BorrowRecordVO> records = borrowService.getBorrowRecordByName("");
        log.info("查询成功");
        return Result.success(records);
    }
    @GetMapping("/{name}")
    public Result getBorrowRecordByReaderId(@PathVariable("name") String name){
        List<BorrowRecordVO> records = borrowService.getBorrowRecordByName(name);
        if(records.isEmpty()){
            log.info("查询失败，内容为空");
            return Result.error("无相关数据");
        }
        log.info("查询成功");
        return Result.success(records);
    }

    @GetMapping("/reader/{id}")
    public Result getBorrowRecordByReaderId(@PathVariable("id") Integer id){
        List<BorrowRecordVO> records = borrowService.getBorrowRecordById(id);
        if(records.isEmpty()){
            log.info("查询失败，内容为空");
            return Result.error("无相关数据");
        }
        log.info("查询成功");
        return Result.success(records);
    }
    @PostMapping("/page")
    public Result pageSearch(@RequestBody BorrowPageDTO borrowPageDTO){
        PageResult page = borrowService.pageSearch(borrowPageDTO);
        return Result.success(page);
    }
    @PostMapping
    public Result createRecord(@RequestBody BorrowRecordDTO borrowRecordDTO){
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
    @PutMapping
    public Result updateRecordById(@RequestBody BorrowRecord borrowRecord){
        log.info("更新id{}的记录",borrowRecord.getId());
        borrowService.Update(borrowRecord);
        return Result.success();
    }
    @GetMapping("/return/{id}")
    public Result returnBook(@PathVariable("id") Integer id){
        log.info("归还id{}书",id);
        borrowService.ReturnBook(id);
        return Result.success();
    }

}
