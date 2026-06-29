-- ============================================================
-- ⚡ LightningDeal 秒杀系统 - 数据库初始化脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS `lightning_deal` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `lightning_deal`;

-- ===== 用户表 =====
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    VARCHAR(32)  NOT NULL                COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL                COMMENT '密码（加密存储）',
    `phone`       VARCHAR(16)  DEFAULT NULL            COMMENT '手机号',
    `email`       VARCHAR(64)  DEFAULT NULL            COMMENT '邮箱',
    `nickname`    VARCHAR(32)  DEFAULT NULL            COMMENT '昵称',
    `avatar`      VARCHAR(256) DEFAULT NULL            COMMENT '头像URL',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ===== 秒杀活动表 =====
DROP TABLE IF EXISTS `seckill_activity`;
CREATE TABLE `seckill_activity` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`             VARCHAR(128) NOT NULL                COMMENT '活动名称',
    `goods_id`         BIGINT       DEFAULT NULL            COMMENT '商品ID',
    `goods_name`       VARCHAR(128) NOT NULL                COMMENT '商品名称',
    `goods_image`      VARCHAR(256) DEFAULT NULL            COMMENT '商品图片URL',
    `goods_description` VARCHAR(512) DEFAULT NULL           COMMENT '商品描述',
    `original_price`   DECIMAL(10,2) NOT NULL               COMMENT '原价',
    `flash_price`      DECIMAL(10,2) NOT NULL               COMMENT '秒杀价',
    `total_stock`      INT          NOT NULL DEFAULT 0      COMMENT '秒杀库存总量',
    `sold_stock`       INT          NOT NULL DEFAULT 0      COMMENT '已秒杀数量',
    `limit_per_user`   INT          NOT NULL DEFAULT 1      COMMENT '每人限购数量',
    `start_time`       DATETIME     NOT NULL                COMMENT '活动开始时间',
    `end_time`         DATETIME     NOT NULL                COMMENT '活动结束时间',
    `status`           TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-草稿 1-上架 2-进行中 3-已结束',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';

-- ===== 秒杀订单表 =====
DROP TABLE IF EXISTS `seckill_order`;
CREATE TABLE `seckill_order` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`     VARCHAR(32)  NOT NULL                COMMENT '订单编号',
    `user_id`      BIGINT       NOT NULL                COMMENT '用户ID',
    `activity_id`  BIGINT       NOT NULL                COMMENT '活动ID',
    `goods_name`   VARCHAR(128) NOT NULL                COMMENT '商品名称',
    `goods_image`  VARCHAR(256) DEFAULT NULL            COMMENT '商品图片',
    `flash_price`  DECIMAL(10,2) NOT NULL               COMMENT '秒杀价格',
    `quantity`     INT          NOT NULL DEFAULT 1      COMMENT '购买数量',
    `total_amount` DECIMAL(10,2) NOT NULL               COMMENT '订单总金额',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-待支付 1-已支付 2-已取消 3-已退款',
    `pay_time`     DATETIME     DEFAULT NULL            COMMENT '支付时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_user_activity` (`user_id`, `activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

-- ===== 初始化测试数据 =====
INSERT INTO `user` (`username`, `password`, `phone`, `nickname`) VALUES
('admin', '$2b$10$e4oU53QnwG/umBtAOBIMwObnJiqm8CKI/cKkY1nml3g8Zhe/GoJda', '13800138000', '管理员'),
('testuser', '$2b$10$e4oU53QnwG/umBtAOBIMwObnJiqm8CKI/cKkY1nml3g8Zhe/GoJda', '13900139000', '测试用户');

-- 密码都是 123456（BCrypt 加密）
-- 秒杀活动测试数据（活动时间为当前时间到未来）
INSERT INTO `seckill_activity` (`name`, `goods_name`, `original_price`, `flash_price`, `total_stock`, `sold_stock`, `limit_per_user`, `start_time`, `end_time`, `status`) VALUES
('iPhone 15 Pro 限时秒杀', 'iPhone 15 Pro 256GB', 8999.00, 6999.00, 100, 0, 1, DATE_ADD(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 1),
('AirPods Pro 超值抢购', 'AirPods Pro 第二代', 1899.00, 1299.00, 200, 0, 2, DATE_ADD(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 3 HOUR), 1),
('Apple Watch S9 限时特惠', 'Apple Watch Series 9', 2999.00, 2199.00, 150, 0, 1, DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 4 HOUR), 1),
('MacBook Air M3 秒杀', 'MacBook Air M3 16+512', 10999.00, 8999.00, 50, 0, 1, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR), 1);
