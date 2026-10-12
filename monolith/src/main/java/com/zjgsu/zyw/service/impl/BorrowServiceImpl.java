package com.zjgsu.zyw.service.impl;

import com.zjgsu.zyw.mapper.BorrowMapper;
import com.zjgsu.zyw.pojo.BorrowRecord;
import com.zjgsu.zyw.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class BorrowServiceImpl implements BorrowService {

    @Autowired
    private BorrowMapper borrowMapper;

    public List<BorrowRecord> QueryRecordById(Integer id){
         return borrowMapper.QueryRecordById(id);
    }

    @Override
    public Integer CreateRecord(BorrowRecord borrowRecord) {
        return borrowMapper.CreateRecord(borrowRecord);
    }
}
