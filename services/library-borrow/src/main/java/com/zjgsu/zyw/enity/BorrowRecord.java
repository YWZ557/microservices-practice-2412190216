package com.zjgsu.zyw.enity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zjgsu.zyw.constant.DatePatterns;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BorrowRecord {
    /** 主键 */
    private Integer id;

    /** 关联 book.id */
    private Integer bookId;

    /** 关联读者 */
    private Integer readerId;

    /** 借出日期 */
    @JsonFormat(pattern = DatePatterns.DATE)
    private LocalDate borrowDate;

    /** 应还日期 */
    @JsonFormat(pattern = DatePatterns.DATE)
    private LocalDate dueDate;

    /** 实际归还日期，未还为 null */
    @JsonFormat(pattern = DatePatterns.DATE)
    private LocalDate returnDate;

    /** 状态：BORROWED / RETURNED / OVERDUE / LOST */
    private String status;

    /** 逾期天数 */
    private Integer overdueDays;

    /** 罚款金额 */
    private Integer fineAmount;
}
