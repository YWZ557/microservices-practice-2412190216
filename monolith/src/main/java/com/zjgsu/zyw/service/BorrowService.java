package com.zjgsu.zyw.service;

import com.zjgsu.zyw.pojo.BorrowRecord;

import java.util.List;

public interface BorrowService {
    public List<BorrowRecord> QueryRecordById(Integer id);
    public Integer CreateRecord(BorrowRecord borrowRecord);
}
