package com.zjgsu.zyw.controller.user;


import com.zjgsu.zyw.dto.UserPageDTO;
import com.zjgsu.zyw.enity.User;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.UserService;
import com.zjgsu.zyw.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/admin/redaer")
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
