package com.zjgsu.zyw.dto;

import lombok.Data;

/**
 * 查询借阅记录的请求对象（DTO）。
 * 承载 API 输入：按读者 ID 查询其借阅记录。
 */
@Data
public class BorrowQueryRequest {
    /** 读者 ID */
    private Integer readerId;
}
