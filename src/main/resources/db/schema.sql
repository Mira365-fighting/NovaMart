-- NovaMart v2.0 schema
-- 全新安装执行这一个文件即可：
--   mysql --default-character-set=utf8mb4 -uroot -p < src/main/resources/db/schema.sql
-- 从 v1.0 / 重构版旧库升级：先执行上面的 schema.sql（不会动已有表），再执行 db/upgrade.sql。

CREATE DATABASE IF NOT EXISTS `novamart`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `novamart`;

-- ---------------------------------------------------------------- user 用户
CREATE TABLE IF NOT EXISTS `user`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(32) NOT NULL COMMENT '登录名',
    `password`    CHAR(60)    NOT NULL COMMENT 'BCrypt 哈希，绝不存明文',
    `phone`       VARCHAR(20) NOT NULL COMMENT '手机号',
    `role`        VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '角色 USER=前台用户 ADMIN=后台管理员',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '1=正常 0=已禁用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '注册用户';

-- ---------------------------------------------------------------- product 商品
CREATE TABLE IF NOT EXISTS `product`
(
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(128)   NOT NULL COMMENT '商品名',
    `description` VARCHAR(512)            DEFAULT NULL COMMENT '简介',
    `price`       DECIMAL(10, 2) NOT NULL COMMENT '单价',
    `stock`       INT            NOT NULL DEFAULT 0 COMMENT '库存',
    `image_url`   VARCHAR(512)            DEFAULT NULL COMMENT '封面图',
    `category`    VARCHAR(64)             DEFAULT NULL COMMENT '分类',
    `status`      TINYINT        NOT NULL DEFAULT 1 COMMENT '1=在售 0=下架',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    -- 前台按名称模糊搜、按分类筛，这两个字段各建一个索引
    KEY `idx_product_name` (`name`),
    KEY `idx_product_category` (`category`),
    KEY `idx_product_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '在售商品';

-- ---------------------------------------------------------------- cart 购物车
-- (user_id, product_id) 唯一：一个用户对一个商品只有一行，加购同一个商品就是累加数量。
-- 这也让「登录后合并本地临时购物车」可以直接用 INSERT ... ON DUPLICATE KEY UPDATE 一条语句搞定。
CREATE TABLE IF NOT EXISTS `cart`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '所属用户 -> user.id',
    `product_id`  BIGINT   NOT NULL COMMENT '商品 -> product.id',
    `quantity`    INT      NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_user_product` (`user_id`, `product_id`),
    KEY `idx_cart_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '购物车行';

-- ---------------------------------------------------------------- orders 订单主表
-- 表名用复数 orders，因为 order 是 MySQL 保留字，直接叫 order 每次都要加反引号。
-- 状态流转：0 待支付 → 1 已支付(待发货) → 2 已发货 → 3 已完成
--          0/1 可以被用户取消(4) 或被后台关闭(5)，两条路都会把库存加回去。
CREATE TABLE IF NOT EXISTS `orders`
(
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`     VARCHAR(32)    NOT NULL COMMENT '订单号，对外展示，不暴露自增 id',
    `user_id`      BIGINT         NOT NULL COMMENT '下单用户 -> user.id',
    `total_amount` DECIMAL(12, 2) NOT NULL COMMENT '订单总金额',
    `status`       TINYINT        NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已发货 3已完成 4已取消 5已关闭',
    `pay_time`     DATETIME                DEFAULT NULL COMMENT '支付时间',
    `ship_time`    DATETIME                DEFAULT NULL COMMENT '发货时间',
    `finish_time`  DATETIME                DEFAULT NULL COMMENT '完成(确认收货)时间',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orders_order_no` (`order_no`),
    KEY `idx_orders_user` (`user_id`),
    -- 后台按状态筛选订单
    KEY `idx_orders_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';

-- ---------------------------------------------------------------- order_item 订单明细
-- 商品名和单价都是「下单那一刻的快照」，商品后续改价改名不影响历史订单。
CREATE TABLE IF NOT EXISTS `order_item`
(
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`     BIGINT         NOT NULL COMMENT '所属订单 -> orders.id',
    `product_id`   BIGINT         NOT NULL COMMENT '商品 -> product.id',
    `product_name` VARCHAR(128)   NOT NULL COMMENT '下单时的商品名快照',
    `price`        DECIMAL(10, 2) NOT NULL COMMENT '下单时的单价快照',
    `quantity`     INT            NOT NULL COMMENT '数量',
    `subtotal`     DECIMAL(12, 2) NOT NULL COMMENT '小计 = price * quantity',
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order` (`order_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细';

-- ---------------------------------------------------------------- 种子数据
-- INSERT IGNORE + 固定 id，脚本可以重复执行而不会插重复数据。
-- 用户不在 SQL 里造（密码要 BCrypt 哈希），由 DataInitializer 在启动时保证存在：
--   admin / admin123（ADMIN 角色）、user01 / user123（USER 角色）
INSERT IGNORE INTO `product` (`id`, `name`, `description`, `price`, `stock`, `image_url`, `category`)
VALUES (1, 'Xiaomi 15 Pro 手机', '2K 曲面屏，骁龙旗舰芯片', 4999.00, 120,
        'https://cdn.example.com/product/xiaomi-15-pro.jpg', '手机'),
       (2, '华为 Mate 70 手机', '旗舰影像，支持卫星消息', 6499.00, 80,
        'https://cdn.example.com/product/mate-70.jpg', '手机'),
       (3, 'iPhone 16 手机', 'A18 芯片，6.1 英寸超视网膜 XDR 屏', 5999.00, 60,
        'https://cdn.example.com/product/iphone-16.jpg', '手机'),
       (4, 'MacBook Air 13 笔记本电脑', 'M4 芯片，16GB 统一内存，512GB 固态', 8999.00, 35,
        'https://cdn.example.com/product/macbook-air-13.jpg', '电脑'),
       (5, 'ThinkPad X1 Carbon 笔记本电脑', '14 英寸商务轻薄本，1.12kg', 10999.00, 20,
        'https://cdn.example.com/product/x1-carbon.jpg', '电脑'),
       (6, 'AirPods Pro 3 无线耳机', '主动降噪，自适应通透', 1899.00, 200,
        'https://cdn.example.com/product/airpods-pro-3.jpg', '耳机'),
       (7, '索尼 WH-1000XM6 头戴耳机', '行业领先降噪头戴耳机', 2899.00, 45,
        'https://cdn.example.com/product/wh-1000xm6.jpg', '耳机'),
       (8, 'Kindle Paperwhite 电子书阅读器', '7 英寸防眩光屏，16GB 存储', 1099.00, 0,
        'https://cdn.example.com/product/kindle-paperwhite.jpg', '数码'),
       (9, '罗技 MX Master 4 鼠标', '静音微动，多设备一键切换', 799.00, 150,
        'https://cdn.example.com/product/mx-master-4.jpg', '数码'),
       (10, '小米智能手环 10', 'AMOLED 屏，14 天续航', 299.00, 300,
        'https://cdn.example.com/product/mi-band-10.jpg', '数码');
