package com.zjgsu.zyw.service;


import com.zjgsu.zyw.dto.BorrowPageDTO;
import com.zjgsu.zyw.dto.BorrowRecordDTO;
import com.zjgsu.zyw.enity.BorrowRecord;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.vo.BorrowRecordVO;

import java.util.List;

public interface BorrowService {

    List<BorrowRecordVO> getBorrowRecordById(Integer id);

    void CreateRecord(BorrowRecordDTO borrowRecordDTO);

    void DeleteRecordById(Integer id);

    void Update(BorrowRecord borrowRecord);

    List<BorrowRecordVO> getBorrowRecordByName(String name);

    String CheckRecord(BorrowRecordDTO borrowRecordDTO);

    void ReturnBook(Integer id);

    PageResult pageSearch(BorrowPageDTO borrowPageDTO);

    void test(Integer id);
}
