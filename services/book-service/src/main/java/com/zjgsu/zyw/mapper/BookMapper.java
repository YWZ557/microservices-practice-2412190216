package com.zjgsu.zyw.mapper;

import com.zjgsu.zyw.dto.BookPageDTO;
import com.zjgsu.zyw.enity.Book;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BookMapper {
    Book list(Integer id);

    void setStatusById(Integer id,String status);

    List<Book> getBook(BookPageDTO bookPageDTO);

    void create(Book book);

    void deleteById(Integer id);

    void updateById(Book book);
}
