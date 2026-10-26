package com.zjgsu.zyw.client.dto;

import lombok.Data;

/**
 * 读者服务返回的读者信息（只保留借阅服务需要的字段）。
 */
@Data
public class ReaderInfo {

    private Integer id;

    private String name;

    private String accountType;

    private Integer status;
}
