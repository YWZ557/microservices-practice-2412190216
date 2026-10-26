package com.zjgsu.zyw.client.dto;

import lombok.Data;

/**
 * 下游服务统一响应结构。
 */
@Data
public class RemoteResult<T> {

    private Integer code;

    private String msg;

    private T data;
}
