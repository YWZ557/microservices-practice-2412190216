package com.zjgsu.zyw.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zjgsu.zyw.constant.DatePatterns;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 读者/账号表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Integer id;

    /** 登录名 */
    private String name;

    /** 账号类型：ADMIN 管理员 / READER 读者 */
    private String accountType;

    /** 状态：1 正常 / 0 禁用 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = DatePatterns.DATE_TIME)
    private LocalDateTime createTime;
}
