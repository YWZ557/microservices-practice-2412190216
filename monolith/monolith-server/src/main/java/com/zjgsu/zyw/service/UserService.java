package com.zjgsu.zyw.service;

import com.zjgsu.zyw.dto.UserLoginDTO;
import com.zjgsu.zyw.dto.UserPageDTO;
import com.zjgsu.zyw.enity.User;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.vo.UserVO;

public interface UserService {
    UserVO getUser();

    PageResult pageSearch(UserPageDTO userPageDTO);

    void createUser(User user);

    void setType(Integer id);

    void setStatus(Integer id);

    String adminLogin(UserLoginDTO userLoginDTO);

    String readerLogin(UserLoginDTO userLoginDTO);
}
