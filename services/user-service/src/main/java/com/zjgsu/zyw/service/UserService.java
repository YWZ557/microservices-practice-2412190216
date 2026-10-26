package com.zjgsu.zyw.service;

import com.zjgsu.zyw.dto.UserLoginDTO;
import com.zjgsu.zyw.dto.UserPageDTO;
import com.zjgsu.zyw.enity.User;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.vo.UserVO;

import java.util.List;

public interface UserService {
    UserVO getUser();

    PageResult pageSearch(UserPageDTO userPageDTO);

    void createUser(User user);

    void setType(Integer id);

    void setStatus(Integer id);

    String adminLogin(UserLoginDTO userLoginDTO);

    String readerLogin(UserLoginDTO userLoginDTO);

    /**
     * 按 ID 查询读者，供其它服务内部调用。
     */
    UserVO getUserById(Integer id);

    /**
     * 按姓名模糊查询读者，供借阅服务按姓名筛选。
     */
    List<UserVO> searchByName(String name);

    /**
     * 批量按 ID 查询读者，供借阅服务回填姓名。
     */
    List<UserVO> getUsersByIds(List<Integer> ids);
}
