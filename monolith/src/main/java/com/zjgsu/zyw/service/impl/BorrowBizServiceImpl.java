package com.zjgsu.zyw.service.impl;

import com.zjgsu.zyw.dto.BorrowRequest;
import com.zjgsu.zyw.pojo.Book;
import com.zjgsu.zyw.pojo.BorrowRecord;
import com.zjgsu.zyw.repository.BookRepository;
import com.zjgsu.zyw.repository.BorrowRecordRepository;
import com.zjgsu.zyw.service.BorrowBizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 借阅业务服务实现，承载两条 Service 业务规则：
 * BR-01：单名读者当前在借数量不得超过上限（MAX_BORROW_LIMIT）。
 * BR-02：图书必须处于 AVAILABLE 状态且 available_count > 0 才允许借出。
 */
@Service
public class BorrowBizServiceImpl implements BorrowBizService {

    /** 读者最大在借数量 */
    private static final int MAX_BORROW_LIMIT = 5;
    /** 借阅天数 */
    private static final int BORROW_DAYS = 30;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Override
    @Transactional
    public BorrowRecord borrow(BorrowRequest request) {
        // BR-02：图书必须存在且在馆（AVAILABLE）且库存大于 0
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new IllegalStateException("图书不存在"));
        if (!"AVAILABLE".equals(book.getStatus()) || book.getAvailableCount() <= 0) {
            throw new IllegalStateException("图书不在馆或库存不足");
        }

        // BR-01：读者当前在借数量不得超过上限
        List<BorrowRecord> borrowed = borrowRecordRepository.findByReaderId(request.getReaderId());
        long activeCount = borrowed.stream()
                .filter(r -> "BORROWED".equals(r.getStatus()))
                .count();
        if (activeCount >= MAX_BORROW_LIMIT) {
            throw new IllegalStateException("读者在借数量已达上限：" + MAX_BORROW_LIMIT);
        }

        // 生成借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setBookId(book.getId());
        record.setReaderId(request.getReaderId());
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(BORROW_DAYS));
        record.setStatus("BORROWED");
        record.setOverdueDays(0);
        record.setFineAmount(0);
        borrowRecordRepository.save(record);

        // 扣减库存
        book.setAvailableCount(book.getAvailableCount() - 1);
        bookRepository.save(book);

        return record;
    }
}
