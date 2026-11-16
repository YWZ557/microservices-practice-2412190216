-- ============================================================
-- 借阅库：library_borrow（独立账号 borrow_svc）
-- 容器首次启动时自动执行；本地重复执行也安全
-- ============================================================

CREATE DATABASE IF NOT EXISTS library_borrow
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'borrow_svc'@'%' IDENTIFIED BY 'borrow_pass';
ALTER USER 'borrow_svc'@'%' IDENTIFIED BY 'borrow_pass';
GRANT ALL PRIVILEGES ON library_borrow.* TO 'borrow_svc'@'%';
FLUSH PRIVILEGES;

USE library_borrow;

CREATE TABLE IF NOT EXISTS borrow_record (
    id           INT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    book_id      INT         DEFAULT NULL COMMENT '关联 book.id',
    reader_id    INT         DEFAULT NULL COMMENT '关联 reader.id',
    borrow_date  DATE        DEFAULT NULL COMMENT '借出日期',
    due_date     DATE        DEFAULT NULL COMMENT '应还日期',
    return_date  DATE        DEFAULT NULL COMMENT '实际归还日期',
    status       VARCHAR(16) DEFAULT NULL COMMENT 'BORROWED/RETURNED/OVERDUE/LOST',
    overdue_days INT         DEFAULT 0 COMMENT '逾期天数',
    fine_amount  INT         DEFAULT 0 COMMENT '罚款金额',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

INSERT INTO borrow_record (id, book_id, reader_id, borrow_date, due_date, return_date, status, overdue_days, fine_amount)
VALUES (1, 3, 2, '2026-10-01', '2026-12-31', NULL, 'BORROWED', 0, 0)
ON DUPLICATE KEY UPDATE status = VALUES(status);
