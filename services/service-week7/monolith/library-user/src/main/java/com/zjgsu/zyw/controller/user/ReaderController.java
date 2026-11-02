package com.zjgsu.zyw.controller.user;

import com.zjgsu.zyw.dto.UserLoginDTO;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/reader")
public class ReaderController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody UserLoginDTO userLoginDTO){
        String token = userService.readerLogin(userLoginDTO);
        return Result.success(token);
    }
    @GetMapping("/loginout")
    public Result loginOut(){
        return Result.success();
    }

}
