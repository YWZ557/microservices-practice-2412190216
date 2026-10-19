package com.zjgsu.zyw.service.impl;

import com.zjgsu.zyw.constant.StatusConstant;
import com.zjgsu.zyw.mapper.BorrowMapper;
import com.zjgsu.zyw.pojo.dto.BorrowRecordDTO;
import com.zjgsu.zyw.pojo.enity.BorrowRecord;
import com.zjgsu.zyw.service.BorrowService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class BorrowServiceImpl implements BorrowService {

    @Autowired
    private BorrowMapper borrowMapper;

    public List<BorrowRecord> getBorrowRecordById(Integer id){
        return borrowMapper.getBorrowRecordById(id);
    }

    @Override
    public void CreateRecord(BorrowRecordDTO borrowRecordDTO) {
        BorrowRecord borrowRecord = new BorrowRecord();
        BeanUtils.copyProperties(borrowRecordDTO,borrowRecord);
        borrowRecord.setBorrowDate(LocalDate.now());
        borrowRecord.setStatus(StatusConstant.BORROWED);
        borrowMapper.CreateRecord(borrowRecord);
    }
}
