package com.zjgsu.zyw.dto;

import lombok.Data;

@Data
public class BorrowPageDTO {
    private int page;

    private int pageSize;

    private String name;

    private Integer readerId;

    private String status;

}
