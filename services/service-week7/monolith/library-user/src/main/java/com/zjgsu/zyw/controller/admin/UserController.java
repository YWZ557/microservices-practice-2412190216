package com.zjgsu.zyw.controller.admin;


import com.zjgsu.zyw.dto.UserPageDTO;
import com.zjgsu.zyw.enity.User;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.UserService;
import com.zjgsu.zyw.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user/admin")
@RestController("adminUserController")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Result getUserById(@PathVariable("id") Integer id){
       UserVO userVO = userService.getUserById(id);
       return Result.success(userVO);
    }
    @PostMapping("/page")
    public Result pageSearch(@RequestBody UserPageDTO userPageDTO){
        PageResult pageResult = userService.pageSearch(userPageDTO);
        return Result.success(pageResult);
    }
    @PostMapping("/create")
    public Result createUser(@RequestBody User user){
        userService.createUser(user);
        return Result.success();
    }
    @GetMapping("/type/{id}")
    public Result setType(@PathVariable("id") Integer id){
        userService.setType(id);
        return Result.success();
    }
    @GetMapping("/status/{id}")
    public Result setStatus(@PathVariable("id") Integer id){
        userService.setStatus(id);
        return Result.success();
    }


}
