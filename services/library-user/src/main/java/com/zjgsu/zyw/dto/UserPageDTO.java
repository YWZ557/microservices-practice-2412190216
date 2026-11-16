package com.zjgsu.zyw.dto;

import lombok.Data;

@Data
public class UserPageDTO {
    private Integer page;

    private Integer pageSize;

    private String name;

    private String accountType;

    private String status;
}
