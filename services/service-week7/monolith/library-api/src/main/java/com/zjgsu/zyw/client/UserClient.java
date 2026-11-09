package com.zjgsu.zyw.client;

import com.zjgsu.zyw.enity.Book;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "library-user", fallbackFactory = UserFallbackFactory.class)
public interface UserClient {
    @GetMapping("/user/admin/{id}")
    Result<UserVO> getUserById(@PathVariable("id") Integer id);
}
