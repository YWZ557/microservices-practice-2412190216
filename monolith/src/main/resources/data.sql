-- ============================================================
-- 测试数据初始化脚本（可重复执行 / 幂等）
-- 使用固定主键 + ON DUPLICATE KEY UPDATE，重复运行不会产生重复数据。
-- ============================================================

-- 图书表（如不存在则创建）
CREATE TABLE IF NOT EXISTS book (
    id              INT           NOT NULL AUTO_INCREMENT COMMENT '主键',
    title           VARCHAR(255)  DEFAULT NULL COMMENT '书名',
    author          VARCHAR(255)  DEFAULT NULL COMMENT '作者',
    publisher       VARCHAR(255)  DEFAULT NULL COMMENT '出版社',
    category        VARCHAR(255)  DEFAULT NULL COMMENT '分类',
    total_count     INT           DEFAULT NULL COMMENT '馆藏总册数',
    available_count INT           DEFAULT NULL COMMENT '可借数量',
    status          VARCHAR(50)   DEFAULT NULL COMMENT '状态',
    location        VARCHAR(255)  DEFAULT NULL COMMENT '馆藏位置',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 借阅记录表（如不存在则创建）
CREATE TABLE IF NOT EXISTS borrow_record (
    id            INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    book_id       INT          DEFAULT NULL COMMENT '关联 book.id',
    reader_id     INT          DEFAULT NULL COMMENT '关联读者',
    borrow_date   DATE         DEFAULT NULL COMMENT '借出日期',
    due_date      DATE         DEFAULT NULL COMMENT '应还日期',
    return_date   DATE         DEFAULT NULL COMMENT '实际归还日期',
    status        VARCHAR(50)  DEFAULT NULL COMMENT '状态',
    overdue_days  INT          DEFAULT NULL COMMENT '逾期天数',
    fine_amount   INT          DEFAULT NULL COMMENT '罚款金额',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 图书数据（幂等插入）
INSERT INTO book (id, title, author, publisher, category, total_count, available_count, status, location)
VALUES (1, 'Java 编程思想', 'Bruce Eckel', '机械工业出版社', '编程', 5, 3, 'AVAILABLE', 'A-01'),
       (2, '深入理解 Java 虚拟机', '周志明', '机械工业出版社', '编程', 4, 4, 'AVAILABLE', 'A-02'),
       (3, 'Spring 实战', 'Craig Walls', '人民邮电出版社', '框架', 3, 2, 'AVAILABLE', 'B-01')
ON DUPLICATE KEY UPDATE
    title           = VALUES(title),
    author          = VALUES(author),
    publisher       = VALUES(publisher),
    category        = VALUES(category),
    total_count     = VALUES(total_count),
    available_count = VALUES(available_count),
    status          = VALUES(status),
    location        = VALUES(location);

-- 借阅记录数据（幂等插入）
INSERT INTO borrow_record (id, book_id, reader_id, borrow_date, due_date, return_date, status, overdue_days, fine_amount)
VALUES (1, 1, 1001, '2026-10-01', '2026-10-15', NULL, 'BORROWED', 0, 0),
       (2, 2, 1001, '2026-09-20', '2026-10-05', '2026-10-06', 'RETURNED', 1, 2),
       (3, 3, 1002, '2026-10-10', '2026-10-24', NULL, 'BORROWED', 0, 0)
ON DUPLICATE KEY UPDATE
    book_id      = VALUES(book_id),
    reader_id    = VALUES(reader_id),
    borrow_date  = VALUES(borrow_date),
    due_date     = VALUES(due_date),
    return_date  = VALUES(return_date),
    status       = VALUES(status),
    overdue_days = VALUES(overdue_days),
    fine_amount  = VALUES(fine_amount);
