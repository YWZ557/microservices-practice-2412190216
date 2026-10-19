package com.zjgsu.zyw.mapper;

import com.zjgsu.zyw.pojo.enity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface BorrowMapper {

    List<BorrowRecord> getBorrowRecordById(Integer readerId);

    void CreateRecord(BorrowRecord borrowRecord);
}
