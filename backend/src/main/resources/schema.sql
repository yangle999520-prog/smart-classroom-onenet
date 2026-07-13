-- ========================================
-- 完整建库 + 建表脚本（在Navicat中执行）
-- ========================================

-- 第一步：创建数据库（如已存在请跳过）
DROP DATABASE IF EXISTS `iot_classroom`;
CREATE DATABASE `iot_classroom`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 第二步：使用数据库
USE `iot_classroom`;

-- 第三步：创建传感器数据表
CREATE TABLE IF NOT EXISTS `sensor_data` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键，自增ID',
    `temperature` FLOAT NOT NULL COMMENT '温度值(°C)',
    `light` INT NOT NULL COMMENT '光照值(%)',
    `led_status` TINYINT NOT NULL COMMENT 'LED灯状态(0=关, 1=开)',
    `mode` TINYINT NOT NULL DEFAULT 0 COMMENT '工作模式(0=自动, 1=手动)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '数据采集时间',
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器数据表';

-- ========================================
-- 清空数据并重置自增ID
-- ========================================
-- TRUNCATE TABLE `iot_classroom`.`sensor_data`;

-- ========================================
-- 只重置自增ID（不清除数据）
-- ========================================
-- ALTER TABLE `iot_classroom`.`sensor_data` AUTO_INCREMENT = 1;
