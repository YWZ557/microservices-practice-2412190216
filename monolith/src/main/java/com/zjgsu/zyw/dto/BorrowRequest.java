package com.zjgsu.zyw.dto;

import lombok.Data;

/**
 * 借书请求对象（DTO），承载 API 输入。
 */
@Data
public class BorrowRequest {
    /** 图书 ID */
    private Integer bookId;
    /** 读者 ID */
    private Integer readerId;
}
