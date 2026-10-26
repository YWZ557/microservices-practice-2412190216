package com.zjgsu.zyw.dto;

import lombok.Data;

@Data
public class ReaderBorrowPageDTO {
    private int page;

    private int pageSize;

    private String name;

    private Integer readerId;

    private String status;

}
