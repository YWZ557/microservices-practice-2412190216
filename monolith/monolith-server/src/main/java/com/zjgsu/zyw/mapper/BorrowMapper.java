package com.zjgsu.zyw.mapper;


import com.zjgsu.zyw.enity.BorrowRecord;
import com.zjgsu.zyw.vo.BorrowRecordVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface BorrowMapper {

    BorrowRecord getBorrowRecordById(Integer id);

    List<BorrowRecordVO> getBorrowRecord(BorrowRecordVO borrowRecordVO);

    void createRecord(BorrowRecord borrowRecord);

    void deleteRecordById(Integer id);

    void updateRecordById(BorrowRecord borrowRecord);

    Integer countByReaderId(Integer readerId);
}
