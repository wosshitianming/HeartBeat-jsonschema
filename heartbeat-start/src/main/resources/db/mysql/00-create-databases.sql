-- HeartBeat 环境数据库初始化脚本（MySQL 8.0）
-- 仅创建数据库，不创建数据库账号，也不写入任何密码。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `heartbeat_dev`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS `heartbeat_test`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS `heartbeat_pre`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS `heartbeat_gray`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS `heartbeat`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
