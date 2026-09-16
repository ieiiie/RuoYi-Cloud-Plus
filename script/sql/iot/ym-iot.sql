-- YM IoT 完整初始化入口。
-- 使用 mysql 客户端从仓库根目录执行：mysql ... < script/sql/iot/ym-iot.sql
CREATE DATABASE IF NOT EXISTS `ym-iot`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `ym-iot`;

SOURCE script/sql/iot/ym-iot-domain-schema.sql;
