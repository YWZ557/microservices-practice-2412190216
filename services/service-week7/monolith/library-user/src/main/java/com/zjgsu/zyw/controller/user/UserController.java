package com.zjgsu.zyw.controller.user;


import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.UserService;
import com.zjgsu.zyw.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user/reader")
@RestController("readerUserController")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public Result getUser(){
        UserVO userVO = userService.getUser();
        return Result.success(userVO);
    }


}
