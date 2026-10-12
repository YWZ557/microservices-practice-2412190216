package com.zjgsu.zyw.pojo;

import lombok.Data;

@Data
public class Result {
    private Integer code; //编码：1成功，0为失败
    private String msg; //提示信息
    private Object data; //返回数据

    //成功响应（不带返回数据）
    public static Result success() {
        Result result = new Result();
        result.code = 1;
        result.msg = "success";
        return result;
    }

    //成功响应（带数据，常用！）
    public static Result success(Object data) {
        Result result = new Result();
        result.code = 1;
        result.msg = "success";
        result.data = data;
        return result;
    }

    //失败响应
    public static Result error(String msg) {
        Result result = new Result();
        result.code = 0;
        result.msg = msg;
        return result;
    }
}
