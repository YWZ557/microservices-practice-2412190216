package com.zjgsu.zyw.dto;

import lombok.Data;

@Data
public class UserPageDTO {
    private int page;

    private int pageSize;

    private String name;

    private String accountType;

    private String status;
}
