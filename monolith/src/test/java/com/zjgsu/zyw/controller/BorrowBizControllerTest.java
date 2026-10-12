package com.zjgsu.zyw.controller;

import com.zjgsu.zyw.dto.BorrowRequest;
import com.zjgsu.zyw.pojo.Book;
import com.zjgsu.zyw.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 正常 / 错误响应测试（MockMvc 走 /api/biz/borrow）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BorrowBizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        Book book = new Book();
        book.setTitle("Java 编程思想");
        book.setAuthor("Bruce Eckel");
        book.setPublisher("机械工业出版社");
        book.setCategory("编程");
        book.setTotalCount(5);
        book.setAvailableCount(5);
        book.setStatus("AVAILABLE");
        book.setLocation("A-01");
        bookRepository.save(book);
    }

    @Test
    void testBorrowApiSuccess() throws Exception {
        BorrowRequest request = new BorrowRequest();
        request.setBookId(1);
        request.setReaderId(3001);

        mockMvc.perform(post("/api/biz/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.status").value("BORROWED"))
                .andExpect(jsonPath("$.data.bookId").value(1));
    }

    @Test
    void testBorrowApiError() throws Exception {
        // 图书不存在，应返回 code=0 的错误响应
        BorrowRequest request = new BorrowRequest();
        request.setBookId(9999);
        request.setReaderId(3002);

        mockMvc.perform(post("/api/biz/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
