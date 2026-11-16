-- ============================================================
-- 容器初始化：一次创建三个独立数据库、三个账号及各自表
-- 该脚本由 MySQL 容器首次启动时执行
-- 本地重复执行也安全
-- ============================================================

CREATE DATABASE IF NOT EXISTS library_user
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS library_book
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS library_borrow
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'user_svc'@'%' IDENTIFIED BY 'user_pass';
ALTER USER 'user_svc'@'%' IDENTIFIED BY 'user_pass';
CREATE USER IF NOT EXISTS 'book_svc'@'%' IDENTIFIED BY 'book_pass';
ALTER USER 'book_svc'@'%' IDENTIFIED BY 'book_pass';
CREATE USER IF NOT EXISTS 'borrow_svc'@'%' IDENTIFIED BY 'borrow_pass';
ALTER USER 'borrow_svc'@'%' IDENTIFIED BY 'borrow_pass';

GRANT ALL PRIVILEGES ON library_user.* TO 'user_svc'@'%';
GRANT ALL PRIVILEGES ON library_book.* TO 'book_svc'@'%';
GRANT ALL PRIVILEGES ON library_borrow.* TO 'borrow_svc'@'%';
FLUSH PRIVILEGES;

-- ---------------- 读者库 ----------------
USE library_user;

CREATE TABLE IF NOT EXISTS reader (
    id           INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name         VARCHAR(64)  DEFAULT NULL COMMENT '登录名',
    password     VARCHAR(128) DEFAULT NULL COMMENT '密码',
    account_type VARCHAR(16)  DEFAULT NULL COMMENT 'ADMIN/READER',
    status       INT          DEFAULT 1 COMMENT '1 正常 / 0 禁用',
    create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reader_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者/账号表';

INSERT INTO reader (id, name, password, account_type, status, create_time)
VALUES (1, 'admin', '123456', 'ADMIN', 1, NOW()),
       (2, 'xiaoming', '123456', 'READER', 1, NOW()),
       (3, 'test_reader01', 'abc123', 'READER', 1, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ---------------- 图书库 ----------------
USE library_book;

CREATE TABLE IF NOT EXISTS book (
    id        INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    title     VARCHAR(128) DEFAULT NULL COMMENT '书名',
    author    VARCHAR(64)  DEFAULT NULL COMMENT '作者',
    publisher VARCHAR(64)  DEFAULT NULL COMMENT '出版社',
    category  VARCHAR(64)  DEFAULT NULL COMMENT '分类',
    isbn      VARCHAR(32)  DEFAULT NULL COMMENT 'ISBN',
    status    VARCHAR(16)  DEFAULT NULL COMMENT 'AVAILABLE/BORROWED/REMOVED',
    location  VARCHAR(64)  DEFAULT NULL COMMENT '馆藏位置',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

INSERT INTO book (id, title, author, publisher, category, isbn, status, location)
VALUES (1, 'Java 编程思想', 'Bruce Eckel', '机械工业出版社', '编程', '978-7-111-00001', 'AVAILABLE', 'A-01'),
       (2, '深入理解 Java 虚拟机', '周志明', '机械工业出版社', '编程', '978-7-111-00002', 'BORROWED', 'A-02'),
       (3, 'Spring 实战', 'Craig Walls', '人民邮电出版社', '框架', '978-7-111-00003', 'AVAILABLE', 'B-01'),
       (4, '代码整洁之道', 'Robert C. Martin', '人民邮电出版社', '编程', '978-7-111-00004', 'AVAILABLE', 'B-02'),
       (5, '三体', '刘慈欣', '重庆出版社', '科幻', '978-7-111-00005', 'AVAILABLE', 'C-01')
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- ---------------- 借阅库 ----------------
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
