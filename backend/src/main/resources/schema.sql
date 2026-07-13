CREATE TABLE IF NOT EXISTS `sensor_data` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `temperature` FLOAT NOT NULL COMMENT '温度值',
    `light` INT NOT NULL COMMENT '光照值',
    `led_status` TINYINT NOT NULL COMMENT 'LED灯状态(0=关, 1=开)',
    `mode` TINYINT NOT NULL DEFAULT 0 COMMENT '工作模式(0=自动, 1=手动)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器数据表';
