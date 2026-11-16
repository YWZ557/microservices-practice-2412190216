-- ============================================================
-- 读者库：library_user（独立账号 user_svc）
-- 容器首次启动时自动执行；本地重复执行也安全
-- ============================================================

CREATE DATABASE IF NOT EXISTS library_user
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'user_svc'@'%' IDENTIFIED BY 'user_pass';
ALTER USER 'user_svc'@'%' IDENTIFIED BY 'user_pass';
GRANT ALL PRIVILEGES ON library_user.* TO 'user_svc'@'%';
FLUSH PRIVILEGES;

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
VALUES (1, 'admin', MD5('123456'), 'ADMIN', 1, NOW()),
       (2, 'xiaoming', MD5('123456'), 'READER', 1, NOW()),
       (3, 'test_reader01', MD5('123456'), 'READER', 1, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);
