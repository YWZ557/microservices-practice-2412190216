package com.zjgsu.zyw.service;

import com.zjgsu.zyw.pojo.dto.BorrowRecordDTO;
import com.zjgsu.zyw.pojo.enity.BorrowRecord;

import java.util.List;

public interface BorrowService {
    public List<BorrowRecord> getBorrowRecordById(Integer id);
    public void CreateRecord(BorrowRecordDTO borrowRecordDTO);
}
