package com.zjgsu.zyw.service;

import com.zjgsu.zyw.dto.BorrowRequest;
import com.zjgsu.zyw.pojo.Book;
import com.zjgsu.zyw.pojo.BorrowRecord;
import com.zjgsu.zyw.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Service 业务规则测试（BR-01 借阅上限、BR-02 在馆校验）。
 */
@SpringBootTest
@Transactional
class BorrowBizServiceTest {

    @Autowired
    private BorrowBizService borrowBizService;

    @Autowired
    private BookRepository bookRepository;

    /** 保存后的图书 ID 列表，避免依赖自增主键的具体值 */
    private final List<Integer> bookIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        bookIds.clear();
        // 准备 6 本在馆图书，用于借阅与上限测试
        for (int i = 1; i <= 6; i++) {
            Book book = new Book();
            book.setTitle("测试图书" + i);
            book.setAuthor("作者" + i);
            book.setPublisher("出版社");
            book.setCategory("测试");
            book.setTotalCount(5);
            book.setAvailableCount(5);
            book.setStatus("AVAILABLE");
            book.setLocation("T-0" + i);
            Book saved = bookRepository.saveAndFlush(book);
            bookIds.add(saved.getId());
        }
    }

    @Test
    void testBorrowSuccess() {
        BorrowRequest request = new BorrowRequest();
        request.setBookId(bookIds.get(0));
        request.setReaderId(2001);

        BorrowRecord record = borrowBizService.borrow(request);

        assertNotNull(record.getId());
        assertEquals("BORROWED", record.getStatus());
        // BR 校验通过后库存应减一：初始 5 -> 4
        Book book = bookRepository.findById(bookIds.get(0)).orElseThrow();
        assertEquals(4, book.getAvailableCount());
    }

    @Test
    void testBorrowRejectBookNotAvailable() {
        // 将第二本图书改为已借出且无库存
        Book book = bookRepository.findById(bookIds.get(1)).orElseThrow();
        book.setStatus("BORROWED");
        book.setAvailableCount(0);
        bookRepository.saveAndFlush(book);

        BorrowRequest request = new BorrowRequest();
        request.setBookId(bookIds.get(1));
        request.setReaderId(2002);

        // BR-02：图书不在馆，应抛异常
        assertThrows(IllegalStateException.class, () -> borrowBizService.borrow(request));
    }

    @Test
    void testBorrowRejectOverLimit() {
        // 读者 2003 连续借满上限（前 5 本）
        for (int i = 0; i < 5; i++) {
            BorrowRequest request = new BorrowRequest();
            request.setBookId(bookIds.get(i));
            request.setReaderId(2003);
            borrowBizService.borrow(request);
        }
        // 第 6 本应被 BR-01 拒绝
        BorrowRequest over = new BorrowRequest();
        over.setBookId(bookIds.get(5));
        over.setReaderId(2003);

        assertThrows(IllegalStateException.class, () -> borrowBizService.borrow(over));
    }
}
