package com.zjgsu.zyw.pojo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BorrowRecordDTO {
    /** 关联 book.id */
    private Integer bookId;

    /** 关联读者 */
    private Integer readerId;


    /** 应还日期 */
    private LocalDate dueDate;


}
