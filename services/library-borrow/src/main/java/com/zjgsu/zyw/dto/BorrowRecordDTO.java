package com.zjgsu.zyw.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zjgsu.zyw.constant.DatePatterns;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BorrowRecordDTO {
    /** 关联 book.id */
    private Integer bookId;

    /** 关联读者 */
    private Integer readerId;


    /** 应还日期 */
    @JsonFormat(pattern = DatePatterns.DATE)
    private LocalDate dueDate;


}
