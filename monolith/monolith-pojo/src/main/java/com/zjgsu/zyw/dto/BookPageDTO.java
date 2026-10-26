package com.zjgsu.zyw.dto;

import lombok.Data;

@Data
public class BookPageDTO {
    private int page;

    private int pageSize;

    private Integer bookId;

    private String title;

    private String category;

    private String status;
}
