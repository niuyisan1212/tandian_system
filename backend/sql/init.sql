-- ============================================
-- 探店路线规划系统 - 数据库初始化脚本
-- 版本: V1.0
-- 日期: 2026-06-08
-- 数据库: MySQL 8.0
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `tandian_system` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `tandian_system`;

-- ============================================
-- 1. 创建店铺表 (shop)
-- ============================================
DROP TABLE IF EXISTS `shop`;

CREATE TABLE `shop` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '店铺名称',
  `category` VARCHAR(50) DEFAULT '美食' COMMENT '店铺类别（美食/娱乐/购物/服务/其他）',
  `address` VARCHAR(255) NOT NULL COMMENT '店铺地址',
  `longitude` DECIMAL(10,7) NOT NULL COMMENT '经度（GCJ-02坐标系）',
  `latitude` DECIMAL(10,7) NOT NULL COMMENT '纬度（GCJ-02坐标系）',
  `geo_source` VARCHAR(20) DEFAULT 'manual' COMMENT '坐标来源（geocode/manual/accurate）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `business_hours` VARCHAR(100) DEFAULT NULL COMMENT '营业时间',
  `remark` TEXT DEFAULT NULL COMMENT '备注信息',
  `visit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '探店状态（0未探店/1已探店）',
  `expire_time` DATE DEFAULT NULL COMMENT '过期时间（NULL表示永不过期）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_visit_status` (`visit_status`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺表';

-- ============================================
-- 2. 创建探店清单表 (shopping_list)
-- ============================================
DROP TABLE IF EXISTS `shopping_list`;

CREATE TABLE `shopping_list` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '清单名称',
  `task_date` DATE NOT NULL COMMENT '任务日期',
  `start_lng` DECIMAL(10,7) DEFAULT NULL COMMENT '起点经度',
  `start_lat` DECIMAL(10,7) DEFAULT NULL COMMENT '起点纬度',
  `start_address` VARCHAR(255) DEFAULT NULL COMMENT '起点地址文本',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0待执行/1已完成/2已取消）',
  `selected_plan_type` VARCHAR(20) DEFAULT NULL COMMENT '选择的方案类型',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_date` (`task_date`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='探店清单表';

-- ============================================
-- 3. 创建清单-店铺关联表 (list_shop)
-- ============================================
DROP TABLE IF EXISTS `list_shop`;

CREATE TABLE `list_shop` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `list_id` BIGINT NOT NULL COMMENT '清单ID',
  `shop_id` BIGINT NOT NULL COMMENT '店铺ID',
  `visit_order` INT DEFAULT NULL COMMENT '访问顺序（1开始）',
  `is_skipped` TINYINT NOT NULL DEFAULT 0 COMMENT '是否跳过（0否/1是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_list_shop` (`list_id`, `shop_id`),
  KEY `idx_list_id` (`list_id`),
  KEY `idx_shop_id` (`shop_id`),
  CONSTRAINT `fk_list_shop_list` FOREIGN KEY (`list_id`) REFERENCES `shopping_list` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_list_shop_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='清单-店铺关联表';

-- ============================================
-- 4. 创建路线方案表 (route_plan)
-- ============================================
DROP TABLE IF EXISTS `route_plan`;

CREATE TABLE `route_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `list_id` BIGINT NOT NULL COMMENT '清单ID',
  `plan_type` VARCHAR(20) NOT NULL COMMENT '方案类型（time_first/cost_first/balanced）',
  `total_distance_m` INT NOT NULL DEFAULT 0 COMMENT '总距离（米）',
  `total_duration_sec` INT NOT NULL DEFAULT 0 COMMENT '总耗时（秒）',
  `total_cost` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总花费（元）',
  `route_json` JSON DEFAULT NULL COMMENT '路线详情JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_list_id` (`list_id`),
  UNIQUE KEY `uk_list_plan_type` (`list_id`, `plan_type`),
  CONSTRAINT `fk_route_plan_list` FOREIGN KEY (`list_id`) REFERENCES `shopping_list` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路线方案表';

-- ============================================
-- 5. 插入测试数据 - 店铺
-- ============================================
INSERT INTO `shop` (`name`, `address`, `longitude`, `latitude`, `geo_source`, `phone`, `business_hours`, `remark`, `visit_status`, `expire_time`) VALUES
('星巴克咖啡', '上海市黄浦区南京东路100号', 121.4858, 31.2345, 'manual', '021-12345678', '08:00-22:00', '环境优雅，适合办公', 0, NULL),
('喜茶GO', '上海市静安区南京西路1266号', 121.4485, 31.2298, 'manual', '021-87654321', '10:00-21:00', '网红奶茶店，排队较长', 0, NULL),
('海底捞火锅', '上海市浦东新区陆家嘴环路168号', 121.4998, 31.2401, 'manual', '021-11112222', '11:00-次日02:00', '服务周到，需要预约', 0, NULL),
('外婆家', '上海市徐汇区漕溪北路8号', 121.4389, 31.1876, 'manual', '021-33334444', '10:00-22:00', '性价比高，菜品丰富', 0, NULL),
('盒马鲜生', '上海市长宁区长宁路1018号', 121.4256, 31.2201, 'manual', '021-55556666', '08:00-22:00', '生鲜超市，可现场加工', 0, NULL),
('无印良品', '上海市普陀区中山北路3300号', 121.4123, 31.2456, 'manual', '021-77778888', '10:00-22:00', '生活用品店，设计简约', 0, NULL),
('宜家家居', '上海市宝山区沪太路1818号', 121.4567, 31.3789, 'manual', '021-99990000', '10:00-21:00', '家居卖场，可以逛很久', 0, NULL),
('鲜丰水果', '上海市闵行区莘建路58号', 121.3890, 31.1122, 'manual', '021-12340000', '07:00-22:00', '新鲜水果，价格实惠', 0, NULL),
('优衣库', '上海市虹口区四川北路1688号', 121.4789, 31.2678, 'manual', '021-56780000', '10:00-22:00', '服装店，款式多样', 0, NULL),
('肯德基', '上海市杨浦区翔殷路1099号', 121.5234, 31.2987, 'manual', '021-90120000', '06:00-23:00', '快餐连锁，方便快捷', 0, NULL);

-- ============================================
-- 6. 插入测试数据 - 清单
-- ============================================
INSERT INTO `shopping_list` (`name`, `task_date`, `start_lng`, `start_lat`, `start_address`, `status`, `selected_plan_type`) VALUES
('上午探店行程', '2026-06-08', 121.4737, 31.2304, '上海市人民广场', 0, NULL),
('下午探店行程', '2026-06-08', 121.4737, 31.2304, '上海市人民广场', 0, NULL);

-- ============================================
-- 7. 插入测试数据 - 清单店铺关联
-- ============================================
INSERT INTO `list_shop` (`list_id`, `shop_id`, `visit_order`, `is_skipped`) VALUES
(1, 1, 1, 0),
(1, 2, 2, 0),
(1, 3, 3, 0),
(1, 4, 4, 0),
(2, 5, 1, 0),
(2, 6, 2, 0),
(2, 7, 3, 0),
(2, 8, 4, 0);

-- ============================================
-- 8. 插入测试数据 - 路线方案
-- ============================================
INSERT INTO `route_plan` (`list_id`, `plan_type`, `total_distance_m`, `total_duration_sec`, `total_cost`, `route_json`) VALUES
(1, 'time_first', 12500, 5400, 15.00, '{"sequence":[1,2,3,4],"segments":[{"from_type":"start","to_shop_id":1,"mode":"subway","instruction":"步行200米至人民广场站，乘1号线2站至南京东路站","duration_sec":600,"distance_m":1500,"cost":3.00},{"from_shop_id":1,"to_shop_id":2,"mode":"bus","instruction":"步行100米至南京东路站，乘公交20路至南京西路站","duration_sec":900,"distance_m":2800,"cost":2.00},{"from_shop_id":2,"to_shop_id":3,"mode":"subway","instruction":"步行150米至南京西路站，乘2号线至陆家嘴站","duration_sec":1200,"distance_m":5200,"cost":5.00},{"from_shop_id":3,"to_shop_id":4,"mode":"walking","instruction":"步行至陆家嘴站，乘9号线至徐家汇站","duration_sec":2700,"distance_m":3000,"cost":5.00}]}'),
(1, 'cost_first', 12500, 7200, 8.00, '{"sequence":[1,2,3,4],"segments":[{"from_type":"start","to_shop_id":1,"mode":"bus","instruction":"步行200米至人民广场站，乘公交18路至南京东路","duration_sec":900,"distance_m":1500,"cost":2.00},{"from_shop_id":1,"to_shop_id":2,"mode":"bus","instruction":"步行100米至南京东路站，乘公交20路至南京西路站","duration_sec":1200,"distance_m":2800,"cost":2.00},{"from_shop_id":2,"to_shop_id":3,"mode":"bus","instruction":"步行150米至南京西路站，乘公交隧道三线至陆家嘴","duration_sec":1800,"distance_m":5200,"cost":2.00},{"from_shop_id":3,"to_shop_id":4,"mode":"bus","instruction":"步行至陆家嘴站，乘公交43路至徐家汇","duration_sec":3300,"distance_m":3000,"cost":2.00}]}'),
(1, 'balanced', 12500, 6000, 12.00, '{"sequence":[1,2,3,4],"segments":[{"from_type":"start","to_shop_id":1,"mode":"subway","instruction":"步行200米至人民广场站，乘1号线2站至南京东路站","duration_sec":600,"distance_m":1500,"cost":3.00},{"from_shop_id":1,"to_shop_id":2,"mode":"walking","instruction":"沿南京路步行街向西步行至南京西路","duration_sec":1500,"distance_m":2800,"cost":0.00},{"from_shop_id":2,"to_shop_id":3,"mode":"subway","instruction":"步行150米至南京西路站，乘2号线至陆家嘴站","duration_sec":1200,"distance_m":5200,"cost":5.00},{"from_shop_id":3,"to_shop_id":4,"mode":"subway","instruction":"步行至陆家嘴站，乘9号线至徐家汇站","duration_sec":2700,"distance_m":3000,"cost":4.00}]}');

-- ============================================
-- 完成
-- ============================================
SELECT '数据库初始化完成！' AS message;
SELECT COUNT(*) AS shop_count FROM shop;
SELECT COUNT(*) AS list_count FROM shopping_list;
