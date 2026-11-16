package com.zjgsu.zyw.constant;

import lombok.Data;

@Data
public final class DatePatterns {
    /** 纯日期：yyyy-MM-dd */
    public static final String DATE = "yyyy-MM-dd";

    /** 日期时间：yyyy-MM-dd HH:mm:ss（不用 ISO 的 T） */
    public static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";

}
