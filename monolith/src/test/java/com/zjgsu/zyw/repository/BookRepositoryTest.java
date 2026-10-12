package com.zjgsu.zyw.repository;

import com.zjgsu.zyw.pojo.Book;
import com.zjgsu.zyw.pojo.BorrowRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository 读写测试（使用 H2 内存库，测试数据在 @BeforeEach 中准备）。
 */
@SpringBootTest
@Transactional
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @BeforeEach
    void setUp() {
        // 准备图书测试数据
        bookRepository.save(newBook("Java 编程思想", "Bruce Eckel", 5, 5));
        bookRepository.save(newBook("深入理解 Java 虚拟机", "周志明", 4, 4));
        bookRepository.save(newBook("Spring 实战", "Craig Walls", 3, 3));
    }

    @Test
    void testReadBook() {
        // 读：验证测试数据
        List<Book> books = bookRepository.findAll();
        assertEquals(3, books.size());
        Book book = books.get(0);
        assertNotNull(book.getId());
        assertEquals("Java 编程思想", book.getTitle());
        assertEquals("AVAILABLE", book.getStatus());
    }

    @Test
    void testWriteBook() {
        // 写：新增一本图书
        Book book = new Book();
        book.setTitle("测试图书");
        book.setAuthor("测试作者");
        book.setPublisher("测试出版社");
        book.setCategory("测试");
        book.setTotalCount(2);
        book.setAvailableCount(2);
        book.setStatus("AVAILABLE");
        book.setLocation("T-01");

        Book saved = bookRepository.save(book);
        assertNotNull(saved.getId());
        assertEquals("测试图书", saved.getTitle());
        assertTrue(bookRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void testWriteBorrowRecord() {
        // 写：新增一条借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setBookId(1);
        record.setReaderId(1001);
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(30));
        record.setStatus("BORROWED");
        record.setOverdueDays(0);
        record.setFineAmount(0);

        BorrowRecord saved = borrowRecordRepository.save(record);
        assertNotNull(saved.getId());

        // 读：按 readerId 查询
        List<BorrowRecord> records = borrowRecordRepository.findByReaderId(1001);
        assertEquals(1, records.size());
        assertEquals("BORROWED", records.get(0).getStatus());
    }

    private Book newBook(String title, String author, int total, int available) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher("机械工业出版社");
        book.setCategory("编程");
        book.setTotalCount(total);
        book.setAvailableCount(available);
        book.setStatus("AVAILABLE");
        book.setLocation("A-01");
        return book;
    }
}
