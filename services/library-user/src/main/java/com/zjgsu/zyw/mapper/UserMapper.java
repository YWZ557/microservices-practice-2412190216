package com.zjgsu.zyw.mapper;

import com.zjgsu.zyw.dto.UserPageDTO;
import com.zjgsu.zyw.enity.User;
import com.zjgsu.zyw.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    List<UserVO> getUser(UserPageDTO userPageDTO);

    UserVO getUserById(Integer id);

    void create(User user);

    void setTypeById(Integer id,String accountType);

    void setStatusById(Integer id,Integer status);

    UserVO getUserByName(String name);

    User getUserByFullName(String name);

    void resetPasswordById(Integer id);

}
