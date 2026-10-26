package com.zjgsu.zyw.client.dto;

import lombok.Data;

/**
 * 图书服务返回的图书信息（只保留借阅服务需要的字段）。
 */
@Data
public class BookInfo {

    private Integer id;

    private String title;

    private String status;
}
