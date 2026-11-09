package com.zjgsu.zyw.client;

import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.result.Result;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class UserFallbackFactory implements FallbackFactory<UserClient> {

    private static final String UNAVAILABLE = "读者服务不可用，请稍后重试";

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public Result getUserById(Integer id) {
                log.warn("调用 library-user 失败：查询读者 id={}", id, cause);
                throw translate(cause);
            }
        };
    }

    private BaseException translate(Throwable cause) {
        if (cause instanceof FeignException ex) {
            int status = ex.status();
            if (status == 404) {
                return new BaseException("读者不存在");
            }
            if (status == 400 || status == 409) {
                return new BaseException("读者服务拒绝了本次请求");
            }
        }
        return new BaseException(UNAVAILABLE);
    }
}
