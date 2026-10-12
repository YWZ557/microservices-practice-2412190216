package com.zjgsu.zyw.mapper;

import com.zjgsu.zyw.pojo.BorrowRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BorrowMapper {
    @Select("select * from borrow_record where reader_id = #{id}")
    public List<BorrowRecord> QueryRecordById(Integer id);
    @Insert("INSERT INTO borrow_record(book_id, reader_id, borrow_date, due_date, return_date, status, overdue_days, fine_amount) VALUES(#{bookId}, #{readerId}, #{borrowDate}, #{dueDate}, #{returnDate}, #{status}, #{overdueDays}, #{fineAmount})")
    public Integer CreateRecord(BorrowRecord borrowRecord);
}
