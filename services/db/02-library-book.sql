-- ============================================================
-- 图书库：library_book（独立账号 book_svc）
-- 容器首次启动时自动执行；本地重复执行也安全
-- ============================================================

CREATE DATABASE IF NOT EXISTS library_book
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'book_svc'@'%' IDENTIFIED BY 'book_pass';
ALTER USER 'book_svc'@'%' IDENTIFIED BY 'book_pass';
GRANT ALL PRIVILEGES ON library_book.* TO 'book_svc'@'%';
FLUSH PRIVILEGES;

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
