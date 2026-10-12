package com.zjgsu.zyw.service;

import com.zjgsu.zyw.dto.BorrowRequest;
import com.zjgsu.zyw.pojo.BorrowRecord;

/**
 * 借阅业务服务（含业务规则校验）。
 * 与既有 MyBatis 版 BorrowService 相互独立，不修改原代码。
 */
public interface BorrowBizService {

    /**
     * 借书：校验 BR-01（在借数量上限）与 BR-02（图书在馆且有库存）后，
     * 生成借阅记录并扣减库存。
     *
     * @return 新生成的借阅记录
     */
    BorrowRecord borrow(BorrowRequest request);
}
