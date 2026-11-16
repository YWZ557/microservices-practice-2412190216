-- ============================================================
-- Monolith 容器初始化：把 dump 里的 library_book / library_borrow /
-- library_user 三个库合并为单库 library，表结构与数据保持 dump 原样。
-- 仅由 MySQL 容器首次启动时执行。
-- ============================================================

CREATE DATABASE IF NOT EXISTS library
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE library;

SET NAMES utf8mb4;

DROP TABLE IF EXISTS borrow_record;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS reader;

-- ---------------- 图书表（原 library_book.book） ----------------
CREATE TABLE book (
    id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    isbn      VARCHAR(32)  DEFAULT NULL COMMENT '国际标准书号，唯一',
    title     VARCHAR(128) DEFAULT NULL COMMENT '书名',
    author    VARCHAR(64)  DEFAULT NULL COMMENT '作者',
    publisher VARCHAR(64)  DEFAULT NULL COMMENT '出版社',
    category  VARCHAR(64)  DEFAULT NULL COMMENT '分类',
    status    VARCHAR(16)  DEFAULT NULL COMMENT 'AVAILABLE在馆/BORROWED已借出/REMOVED已下架',
    location  VARCHAR(64)  DEFAULT NULL COMMENT '馆藏位置（书架号）',
    PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

INSERT INTO book (id, isbn, title, author, publisher, category, status, location) VALUES
    (1, '978-7-111-00001', 'Java 编程思想', 'Bruce Eckel', '机械工业出版社', '编程', 'BORROWED', 'A-03'),
    (2, '978-7-111-00002', '深入理解 Java 虚拟机', '周志明1', '机械工业出版社', '编程', 'BORROWED', 'A-02'),
    (3, '978-7-111-00002', '深入理解 Java 虚拟机', '周志明1', '机械工业出版社', '编程', 'BORROWED', 'A-02'),
    (4, '978-7-111-00004', '代码整洁之道', '罗伯特·马丁', '人民邮电出版社', '计算机', 'AVAILABLE', 'B区-01-01'),
    (5, '978-7-111-00005', '三体', '刘慈欣', '重庆出版社', '科幻', 'AVAILABLE', 'C区-03-01'),
    (6, 'TEST-BORROW-001', '契约测试-借阅', '测试作者', '测试社', '测试', 'AVAILABLE', 'T-01');

-- ---------------- 借阅记录表（原 library_borrow.borrow_record） ----------------
CREATE TABLE borrow_record (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    book_id      BIGINT        DEFAULT NULL COMMENT '关联 book.id',
    reader_id    BIGINT        DEFAULT NULL COMMENT '关联读者',
    borrow_date  DATE          DEFAULT NULL COMMENT '借出日期',
    due_date     DATE          DEFAULT NULL COMMENT '应还日期',
    return_date  DATE          DEFAULT NULL COMMENT '实际归还日期，未还为null',
    status       VARCHAR(16)   DEFAULT NULL COMMENT 'BORROWED/RETURNED/OVERDUE/LOST',
    overdue_days INT           DEFAULT NULL COMMENT '逾期天数',
    fine_amount  DECIMAL(10,2) DEFAULT NULL COMMENT '罚款金额',
    PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

INSERT INTO borrow_record (id, book_id, reader_id, borrow_date, due_date, return_date, status, overdue_days, fine_amount) VALUES
    (4,  3, 2, '2025-12-20', '2026-01-20', '2026-11-02', 'RETURNED',  308, 616.00),
    (5,  4, 4, '2025-11-01', '2025-12-01', NULL,         'LOST',        0,  50.00),
    (6,  1, 1, '2026-01-05', '2026-02-05', '2026-10-23', 'RETURNED',    5,   0.00),
    (10, 1, 1, '2026-10-23', '2026-10-23', '2026-11-09', 'RETURNED',    1,   2.00),
    (13, 1, 1, '2026-10-23', '2026-10-24', '2026-10-23', 'RETURNED',    0,   0.00),
    (14, 2, 1, '2026-10-23', '2026-11-22', '2026-10-23', 'RETURNED',    0,   0.00),
    (16, 5, 1, '2026-10-01', '2026-10-21', '2026-10-16', 'RETURNED',    0,   0.00),
    (17, 6, 1, '2026-10-23', '2026-11-07', NULL,         'BORROWED',    0,   0.00),
    (18, 2, 1, '2026-10-23', '2026-10-24', '2026-10-23', 'RETURNED',    0,   0.00),
    (19, 2, 1, '2026-10-23', '2026-10-31', NULL,         'BORROWED',    0,   0.00),
    (20, 1, 1, '2026-11-08', '2026-11-10', '2026-11-09', 'RETURNED',    0,   0.00),
    (21, 1, 1, '2026-11-08', '2026-11-09', '2026-11-09', 'RETURNED',    0,   0.00),
    (22, 1, 1, '2026-11-09', '2026-11-10', '2026-11-09', 'RETURNED',    0,   0.00),
    (23, 1, 1, '2026-11-09', '2026-12-01', '2026-11-11', 'RETURNED',    0,   0.00),
    (24, 1, 1, '2026-11-11', '2026-11-12', NULL,         'BORROWED',    0,   0.00);

-- ---------------- 读者表（原 library_user.reader） ----------------
CREATE TABLE reader (
    id           INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name         VARCHAR(64)  DEFAULT NULL COMMENT '登录名',
    password     VARCHAR(128) DEFAULT NULL COMMENT '密码',
    account_type VARCHAR(16)  DEFAULT NULL COMMENT 'ADMIN/READER',
    status       INT          DEFAULT '1' COMMENT '1 正常 / 0 禁用',
    create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reader_name (name)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COMMENT='读者/账号表';

INSERT INTO reader (id, name, password, account_type, status, create_time) VALUES
    (1, 'admin',        '123456',  'ADMIN',  1, '2026-10-23 17:49:36'),
    (2, 'xiaoming',     '123456',  'READER', 1, '2026-10-23 17:49:36'),
    (3, 'test_reader01','abc123',  'READER', 1, '2026-10-24 22:44:19'),
    (4, 'zhangsan',     '123456a', 'READER', 1, '2026-10-24 22:44:32'),
    (8, 'wangwu',       '123456a', 'READER', 1, '2026-11-11 16:12:40');
