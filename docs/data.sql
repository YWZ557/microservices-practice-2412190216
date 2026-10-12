create database course;
CREATE DATABASE IF NOT EXISTS library DEFAULT CHARACTER SET utf8mb4;
USE library;

-- 图书表（对应 Book 类）
CREATE TABLE book (
                      id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
                      isbn            VARCHAR(32)   COMMENT '国际标准书号，唯一',
                      title           VARCHAR(128)  COMMENT '书名',
                      author          VARCHAR(64)   COMMENT '作者',
                      publisher       VARCHAR(64)   COMMENT '出版社',
                      category        VARCHAR(64)   COMMENT '分类',
                      total_count     INT           COMMENT '馆藏总册数',
                      available_count INT           COMMENT '可借数量',
                      status          VARCHAR(16)   COMMENT 'AVAILABLE在馆/BORROWED已借出/REMOVED已下架',
                      location        VARCHAR(64)   COMMENT '馆藏位置（书架号）',
                      PRIMARY KEY (id),
                      UNIQUE KEY uk_isbn (isbn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 借阅记录表（对应 BorrowRecord 类）
CREATE TABLE borrow_record (
                               id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
                               book_id      BIGINT         COMMENT '关联 book.id',
                               reader_id    BIGINT         COMMENT '关联读者',
                               borrow_date  DATE           COMMENT '借出日期',
                               due_date     DATE           COMMENT '应还日期',
                               return_date  DATE           COMMENT '实际归还日期，未还为null',
                               status       VARCHAR(16)    COMMENT 'BORROWED/RETURNED/OVERDUE/LOST',
                               overdue_days INT            COMMENT '逾期天数',
                               fine_amount  DECIMAL(10,2)  COMMENT '罚款金额',
                               PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';
INSERT INTO book (isbn, title, author, publisher, category, total_count, available_count, status, location) VALUES
                                                                                                                ('978-7-111-00001', 'Java核心技术·卷I',   '凯·霍斯特曼', '机械工业出版社', '计算机', 5, 3, 'AVAILABLE', 'A区-01-01'),
                                                                                                                ('978-7-111-00002', '深入理解Java虚拟机', '周志明',       '机械工业出版社', '计算机', 3, 0, 'BORROWED',  'A区-01-02'),
                                                                                                                ('978-7-111-00003', 'Spring实战',        '克雷格·沃斯',  '人民邮电出版社', '计算机', 4, 4, 'AVAILABLE', 'A区-02-01'),
                                                                                                                ('978-7-111-00004', '代码整洁之道',      '罗伯特·马丁',  '人民邮电出版社', '计算机', 2, 1, 'AVAILABLE', 'B区-01-01'),
                                                                                                                ('978-7-111-00005', '三体',              '刘慈欣',       '重庆出版社',     '科幻',   6, 6, 'AVAILABLE', 'C区-03-01');
INSERT INTO borrow_record (book_id, reader_id, borrow_date, due_date, return_date, status, overdue_days, fine_amount) VALUES
                                                                                                                          (1, 1001, '2026-01-05', '2026-02-05', NULL,         'BORROWED', 0, 0.00),
                                                                                                                          (2, 1001, '2026-01-10', '2026-02-10', NULL,         'BORROWED', 0, 0.00),
                                                                                                                          (1, 1002, '2025-12-01', '2025-12-31', '2026-01-03', 'RETURNED', 3, 3.00),
                                                                                                                          (3, 1002, '2025-12-20', '2026-01-20', NULL,         'OVERDUE',  15, 15.00),
                                                                                                                          (4, 1003, '2025-11-01', '2025-12-01', NULL,         'LOST',     0, 50.00);