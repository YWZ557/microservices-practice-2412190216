package com.zjgsu.zyw.client;

import com.zjgsu.zyw.enity.Book;
import com.zjgsu.zyw.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "library-book",fallbackFactory = BookFallbackFactory.class)
public interface BookClient {
    @PostMapping("/book/admin/update")
    void updateBook(@RequestBody Book book);
    @GetMapping("/book/admin/{id}")
    Result<Book> getBookById(@PathVariable("id") Integer id);
}
