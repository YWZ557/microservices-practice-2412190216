package com.zjgsu.zyw.service.impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zjgsu.zyw.constant.UserConstant;
import com.zjgsu.zyw.context.BaseContext;
import com.zjgsu.zyw.dto.UserLoginDTO;
import com.zjgsu.zyw.dto.UserPageDTO;
import com.zjgsu.zyw.enity.User;
import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.mapper.UserMapper;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.service.UserService;
import com.zjgsu.zyw.utils.JwtUtil;
import com.zjgsu.zyw.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)\\S{6,20}$");


    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public UserVO getUser() {
        return userMapper.getUserById(BaseContext.getCurrentId());
    }

    @Override
    public PageResult pageSearch(UserPageDTO userPageDTO) {
        PageHelper.startPage(userPageDTO.getPage(), userPageDTO.getPageSize());
        List<UserVO> list = userMapper.getUser(userPageDTO);
        Page<UserVO> p = (Page<UserVO>) list;
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public void createUser(User user) {
        user.setAccountType(UserConstant.ACCOUNT_TYPE_READER);
        user.setCreateTime(LocalDateTime.now());
        user.setStatus(1);
        String password = user.getPassword();
        if (password == null || !PASSWORD.matcher(password).matches()) {
            throw new BaseException("密码需为 6~20 位，且同时包含字母和数字");
        }
        userMapper.create(user);
    }

    @Override
    public void setType(Integer id) {
        UserVO userVO = userMapper.getUserById(id);
        if (UserConstant.ACCOUNT_TYPE_ADMIN.equals(userVO.getAccountType())) {
            userMapper.setTypeById(id, UserConstant.ACCOUNT_TYPE_READER);
            return;
        }
        userMapper.setTypeById(id, UserConstant.ACCOUNT_TYPE_ADMIN);
    }

    @Override
    public void setStatus(Integer id) {
        UserVO userVO = userMapper.getUserById(id);
        if (UserConstant.STATUS_ENABLED.equals(userVO.getStatus())) {
            userMapper.setStatusById(id, UserConstant.STATUS_DISABLED);
            return;
        }
        userMapper.setStatusById(id, UserConstant.STATUS_ENABLED);
    }

    @Override
    public String  adminLogin(UserLoginDTO userLoginDTO) {
        String name = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();
        User user = userMapper.getUserByName(name);

        if (user == null){
            throw new BaseException("用户名错误");
        }
        if(!user.getPassword().equals(password)){
            throw new BaseException("密码错误");
        }
        if(!user.getAccountType().equals(UserConstant.ACCOUNT_TYPE_ADMIN)){
            throw new BaseException("该账号不是管理员");
        }
        if(Objects.equals(user.getStatus(), UserConstant.STATUS_DISABLED)){
            throw new BaseException("账户已被停用，请联系管理员");
        }
        String token = jwtUtil.createToken(user.getId(), user.getAccountType());
        log.info("创建token成功{}",token);
        return token;

    }
    @Override
    public String  readerLogin(UserLoginDTO userLoginDTO) {
        String name = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();
        User user = userMapper.getUserByName(name);

        if (user == null){
            throw new BaseException("用户名错误");
        }
        if(!user.getPassword().equals(password)){
            throw new BaseException("密码错误");
        }
        if(!user.getAccountType().equals(UserConstant.ACCOUNT_TYPE_READER)){
            throw new BaseException("该账号不是读者");
        }
        if(Objects.equals(user.getStatus(), UserConstant.STATUS_DISABLED)){
            throw new BaseException("账户已被停用，请联系管理员");
        }
        String token = jwtUtil.createToken(user.getId(), user.getAccountType());
        log.info("创建token成功{}",token);
        return token;

    }

    @Override
    public UserVO getUserById(Integer id) {
        return userMapper.getUserById(id);
    }
}
