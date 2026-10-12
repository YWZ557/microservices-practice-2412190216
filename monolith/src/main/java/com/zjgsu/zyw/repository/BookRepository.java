package com.zjgsu.zyw.repository;

import com.zjgsu.zyw.pojo.Book;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 图书数据访问接口（Spring Data JPA）。
 * 继承 JpaRepository 即自动获得基础的增删改查能力。
 */
public interface BookRepository extends JpaRepository<Book, Integer> {
}
