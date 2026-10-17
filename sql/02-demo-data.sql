-- ============================================================
-- 演示数据:管理员、商户、店铺、分类、商品
-- 执行:mysql -u root -p clp < sql/02-demo-data.sql
--
-- 说明:
-- 1. 建议在全新数据库上执行;重复执行使用 INSERT IGNORE,不会覆盖已有数据
-- 2. 演示手机号:管理员 13900000000 / 商户 13900000001
-- 3. 普通用户不需要预置:任意手机号验证码登录会自动注册
-- ============================================================

SET NAMES utf8mb4;
START TRANSACTION;

-- 管理员
INSERT IGNORE INTO tb_user (id, phone, nickname, role, status)
VALUES (100000000000000001, '13900000000', '演示管理员', 'ADMIN', 'ACTIVE');

-- 商户账号
INSERT IGNORE INTO tb_user (id, phone, nickname, role, status)
VALUES (100000000000000002, '13900000001', '演示商户', 'MERCHANT', 'ACTIVE');

-- 已审核通过的商户
INSERT IGNORE INTO tb_merchant
    (id, user_id, shop_name, license_no, address, contact_name, contact_phone,
     audit_status, audit_remark, audited_by, audited_at)
VALUES
    (100000000000000010, 100000000000000002, '演示小店', 'DEMO-LICENSE-001',
     '演示市演示区演示路 1 号', '张三', '13900000001',
     'APPROVED', '演示数据', 100000000000000001, NOW());

-- 演示店铺分类(应用启动时会自动补齐 8 个常用分类,已存在的不重复插入)
INSERT IGNORE INTO tb_shop_type (id, name, icon, sort)
VALUES (100000000000000020, '演示分类', '', 99);

-- 店铺
INSERT IGNORE INTO tb_shop
    (id, merchant_id, type_id, name, address, lat, lng, phone,
     open_time, close_time, status, deleted)
VALUES
    (100000000000000030, 100000000000000010, 100000000000000020, '演示小店',
     '演示市演示区演示路 1 号', 39.9042000, 116.4074000, '13900000001',
     '09:00', '21:00', 'OPEN', 0);

-- 商品(价格单位:分)
INSERT IGNORE INTO tb_product
    (id, shop_id, name, price, stock, sold, description, image, status, version, deleted)
VALUES
    (100000000000000101, 100000000000000030, '手工水饺', 1990, 100, 0, '演示商品', '', 'ON_SALE', 0, 0),
    (100000000000000102, 100000000000000030, '冰镇酸梅汤', 800, 50, 0, '演示商品', '', 'ON_SALE', 0, 0),
    (100000000000000103, 100000000000000030, '招牌炒饭', 1590, 80, 0, '演示商品', '', 'ON_SALE', 0, 0);

COMMIT;

-- 校验
SELECT 'users' AS item, COUNT(*) AS cnt
FROM tb_user WHERE id IN (100000000000000001, 100000000000000002)
UNION ALL
SELECT 'merchant', COUNT(*) FROM tb_merchant WHERE id = 100000000000000010
UNION ALL
SELECT 'shop', COUNT(*) FROM tb_shop WHERE id = 100000000000000030
UNION ALL
SELECT 'product', COUNT(*) FROM tb_product WHERE shop_id = 100000000000000030;
