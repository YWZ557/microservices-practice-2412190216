package com.zjgsu.zyw.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "borrow_record")
public class BorrowRecord {
    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 关联 book.id */
    @Column(name = "book_id")
    private Integer bookId;

    /** 关联读者 */
    @Column(name = "reader_id")
    private Integer readerId;

    /** 借出日期 */
    @Column(name = "borrow_date")
    private LocalDate borrowDate;

    /** 应还日期 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 实际归还日期，未还为 null */
    @Column(name = "return_date")
    private LocalDate returnDate;

    /** 状态：BORROWED / RETURNED / OVERDUE / LOST */
    private String status;

    /** 逾期天数 */
    @Column(name = "overdue_days")
    private Integer overdueDays;

    /** 罚款金额 */
    @Column(name = "fine_amount")
    private Integer fineAmount;
}
