package com.zjgsu.zyw.handler;

import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException e){
        String message = e.getMessage();
        log.info("捕获到{}",message);
        if(message.contains("Duplicate entry")){
            String errorMessage = message.split(" ")[2] + "已存在";
            return Result.error(errorMessage);
        }else{
            return Result.error("未知错误");
        }

    }
    @ExceptionHandler
    public Result exceptionHandler(BaseException e){
        String message = e.getMessage();
        log.info("捕获到{}",message);
        return Result.error(message);
    }
}
