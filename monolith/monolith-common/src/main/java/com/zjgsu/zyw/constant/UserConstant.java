package com.zjgsu.zyw.constant;

import lombok.Data;

@Data
public class UserConstant {
    /** 账号类型：管理员 */
    public static final String ACCOUNT_TYPE_ADMIN = "ADMIN";

    /** 账号类型：读者 */
    public static final String ACCOUNT_TYPE_READER = "READER";

    /** 状态：正常 */
    public static final Integer STATUS_ENABLED = 1;

    /** 状态：禁用 */
    public static final Integer STATUS_DISABLED = 0;

    /** 新建读者时的默认密码 */
    public static final String DEFAULT_PASSWORD = "123456";
}
