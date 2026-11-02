package com.zjgsu.zyw.controller.admin;

import com.zjgsu.zyw.dto.UserLoginDTO;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody UserLoginDTO userLoginDTO){
        String token = userService.adminLogin(userLoginDTO);
        return Result.success(token);
    }
    @GetMapping("/loginout")
    public Result loginOut(){
        return Result.success();
    }

}
