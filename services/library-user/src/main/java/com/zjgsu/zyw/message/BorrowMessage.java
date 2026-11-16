package com.zjgsu.zyw.message;

import lombok.Data;


@Data
public final class BorrowMessage {

    /* ---------- 数据不存在 ---------- */
    public static final String BOOK_NOT_FOUND   = "图书不存在";
    public static final String READER_NOT_FOUND = "读者不存在";
    public static final String RECORD_NOT_FOUND = "借阅记录不存在";
    public static final String TURE = "借阅成功";

    /* ---------- 图书状态 ---------- */
    public static final String BOOK_NO_STOCK    = "该图书已无可借库存";
    public static final String BOOK_REMOVED     = "该图书已下架，无法借阅";

    /* ---------- 读者状态 ---------- */
    public static final String READER_FROZEN    = "读者账号已被冻结";
    public static final String BORROW_LIMIT_EXCEEDED = "已达最大可借数量";

    /* ---------- 借阅记录状态 ---------- */
    public static final String ALREADY_BORROWED = "该读者已借阅此书且未归还";
    public static final String ALREADY_RETURNED = "该记录已归还，不能重复操作";
    public static final String NOT_BORROWED     = "该记录不是借阅中状态，无法归还";
    public static final String IS_LOST          = "该记录已标记丢失，不能还书";

    /* ---------- 参数与时间 ---------- */
    public static final String INVALID_DUE_DATE = "应还日期不能早于借出日期";


}

