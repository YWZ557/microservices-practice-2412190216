package com.zjgsu.zyw.controller;

import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.UserService;
import com.zjgsu.zyw.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 供其它微服务调用的读者内部接口。
 * 路径以 /internal 开头，不经过 JWT 拦截器。
 */
@RestController
@RequestMapping("/internal/readers")
public class InternalReaderController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Result getById(@PathVariable("id") Integer id) {
        UserVO user = userService.getUserById(id);
        if (user == null) {
            return Result.error("读者不存在");
        }
        return Result.success(user);
    }

    @GetMapping("/search")
    public Result search(@RequestParam(value = "name", required = false) String name) {
        return Result.success(userService.searchByName(name));
    }

    @PostMapping("/batch")
    public Result batch(@RequestBody List<Integer> ids) {
        return Result.success(userService.getUsersByIds(ids));
    }
}
