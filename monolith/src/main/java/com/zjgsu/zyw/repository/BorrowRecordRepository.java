package com.zjgsu.zyw.repository;

import com.zjgsu.zyw.pojo.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 借阅记录数据访问接口（Spring Data JPA）。
 * 方法名遵循 Spring Data 的命名约定，按 readerId 查询借阅记录。
 */
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Integer> {

    List<BorrowRecord> findByReaderId(Integer readerId);
}
