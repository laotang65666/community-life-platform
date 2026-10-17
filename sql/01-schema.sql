-- ============================================================
-- 面向社区用户的生活服务平台 · 完整建表脚本
-- 由 docs/04-数据模型.md 自动汇总
-- 执行: mysql -u root -p clp < sql/01-schema.sql
-- 要求: MySQL 8.0+ (知识库表使用 ngram 全文索引)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 账号
-- ============================================================
CREATE TABLE `tb_user` (
    `id`            BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花 ID)',
    `phone`         VARCHAR(20)     NOT NULL COMMENT '手机号',
    `password`      VARCHAR(100)    NOT NULL DEFAULT '' COMMENT '预留字段;当前为验证码登录,不存密码',
    `nickname`      VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`        VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '头像 URL',
    `intro`         VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '个人简介',
    `role`          VARCHAR(20)     NOT NULL DEFAULT 'USER' COMMENT '角色:USER / MERCHANT / ADMIN',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE / DISABLED',
    `last_login_at` DATETIME        NULL COMMENT '最后登录时间',
    `deleted`       TINYINT         NOT NULL DEFAULT 0 COMMENT '是否删除:0 否 / 1 是',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_role_status` (`role`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '账号';

-- ============================================================
-- 2. 商户入驻
-- ============================================================
CREATE TABLE `tb_merchant` (
    `id`            BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '申请人用户 ID',
    `shop_name`     VARCHAR(128)    NOT NULL COMMENT '店铺名称',
    `license_no`    VARCHAR(64)     NOT NULL COMMENT '营业执照号',
    `address`       VARCHAR(255)    NOT NULL COMMENT '经营地址',
    `contact_name`  VARCHAR(50)     NOT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20)     NOT NULL COMMENT '联系电话',
    `audit_status`  VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '审核状态:PENDING / APPROVED / REJECTED',
    `audit_remark`  VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '审核意见 / 驳回原因',
    `audited_by`    BIGINT UNSIGNED NULL COMMENT '审核人(管理员)',
    `audited_at`    DATETIME        NULL COMMENT '审核时间',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_audit_status` (`audit_status`, `created_at`),
    UNIQUE KEY `uk_license_no` (`license_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商户入驻申请';

-- ============================================================
-- 3. 店铺
-- ============================================================
CREATE TABLE `tb_shop` (
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商户 ID',
    `type_id`     BIGINT UNSIGNED NOT NULL COMMENT '分类 ID',
    `name`        VARCHAR(128)    NOT NULL COMMENT '店铺名称',
    `address`     VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '地址',
    `lat`         DECIMAL(10, 7)  NULL COMMENT '纬度',
    `lng`         DECIMAL(10, 7)  NULL COMMENT '经度',
    `phone`       VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '联系电话',
    `open_time`   VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '营业开始时间 HH:mm',
    `close_time`  VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '营业结束时间 HH:mm',
    `status`      VARCHAR(20)     NOT NULL DEFAULT 'OPEN' COMMENT '状态:OPEN / CLOSED',
    `deleted`     TINYINT         NOT NULL DEFAULT 0,
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_id` (`merchant_id`),
    KEY `idx_type_status` (`type_id`, `status`),
    KEY `idx_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '店铺';

-- ============================================================
-- 4. 店铺分类
-- ============================================================
CREATE TABLE `tb_shop_type` (
    `id`         BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `name`       VARCHAR(50)     NOT NULL COMMENT '分类名称',
    `icon`       VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '图标 URL',
    `sort`       INT             NOT NULL DEFAULT 0 COMMENT '排序值,越小越靠前',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_sort` (`sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '店铺分类';

-- ============================================================
-- 5. 商品
-- ============================================================
CREATE TABLE `tb_product` (
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `shop_id`     BIGINT UNSIGNED NOT NULL COMMENT '店铺 ID',
    `name`        VARCHAR(128)    NOT NULL COMMENT '商品名称',
    `price`       BIGINT          NOT NULL COMMENT '单价(分)',
    `stock`       INT             NOT NULL DEFAULT 0 COMMENT '库存',
    `sold`        INT             NOT NULL DEFAULT 0 COMMENT '已售数量',
    `description` TEXT            NULL COMMENT '描述',
    `image`       VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '主图 URL',
    `status`      VARCHAR(20)     NOT NULL DEFAULT 'ON_SALE' COMMENT '状态:ON_SALE / OFF_SALE',
    `version`     INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`     TINYINT         NOT NULL DEFAULT 0,
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_shop_status` (`shop_id`, `status`),
    KEY `idx_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品';

-- ============================================================
-- 6. 订单
-- ============================================================
CREATE TABLE `tb_order` (
    `id`            BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `order_no`      VARCHAR(32)     NOT NULL COMMENT '订单号(业务唯一)',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '下单用户',
    `shop_id`       BIGINT UNSIGNED NOT NULL COMMENT '店铺',
    `total_amount`  BIGINT          NOT NULL COMMENT '订单总额(分)',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'CREATED'
                    COMMENT '状态:CREATED / PAID / ACCEPTED / COMPLETED / CANCELLED / CLOSED / REFUNDED',
    `remark`        VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '用户备注',
    `pay_time`      DATETIME        NULL COMMENT '支付时间',
    `accept_time`   DATETIME        NULL COMMENT '接单时间',
    `complete_time` DATETIME        NULL COMMENT '完成时间',
    `cancel_time`   DATETIME        NULL COMMENT '取消时间',
    `cancel_reason` VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '取消原因',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_status_created` (`user_id`, `status`, `created_at`),
    KEY `idx_shop_status_created` (`shop_id`, `status`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单';

-- ============================================================
-- 7. 订单明细
-- ============================================================
CREATE TABLE `tb_order_item` (
    `id`            BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    `product_id`    BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
    `product_name`  VARCHAR(128)    NOT NULL COMMENT '下单时商品名快照',
    `product_image` VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '下单时主图快照',
    `price`         BIGINT          NOT NULL COMMENT '下单时单价(分)',
    `quantity`      INT             NOT NULL COMMENT '数量',
    `amount`        BIGINT          NOT NULL COMMENT '小计(分)',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细';

-- ============================================================
-- 8. 优惠券
-- ============================================================
CREATE TABLE `tb_voucher` (
    `id`           BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `shop_id`      BIGINT UNSIGNED NOT NULL COMMENT '店铺 ID',
    `title`        VARCHAR(128)    NOT NULL COMMENT '标题',
    `sub_title`    VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '副标题',
    `rules`        TEXT            NULL COMMENT '使用规则',
    `pay_value`    BIGINT          NOT NULL COMMENT '支付金额(分)',
    `actual_value` BIGINT          NOT NULL COMMENT '抵扣金额(分)',
    `type`         VARCHAR(20)     NOT NULL DEFAULT 'NORMAL' COMMENT '类型:NORMAL / SECKILL',
    `status`       VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态:DRAFT / PUBLISHED / ENDED',
    `begin_time`   DATETIME        NOT NULL COMMENT '开始时间',
    `end_time`     DATETIME        NOT NULL COMMENT '结束时间',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_status_begin` (`status`, `begin_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '优惠券';

-- ============================================================
-- 9. 秒杀库存
-- ============================================================
CREATE TABLE `tb_seckill_voucher` (
    `id`             BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `voucher_id`     BIGINT UNSIGNED NOT NULL COMMENT '优惠券 ID',
    `stock`          INT             NOT NULL DEFAULT 0 COMMENT '剩余库存',
    `initial_stock`  INT             NOT NULL DEFAULT 0 COMMENT '初始库存(对账基准)',
    `per_user_limit` INT             NOT NULL DEFAULT 1 COMMENT '每人限购',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `begin_time`     DATETIME        NOT NULL COMMENT '抢购开始',
    `end_time`       DATETIME        NOT NULL COMMENT '抢购结束',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_voucher_id` (`voucher_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '秒杀库存';

-- ============================================================
-- 10. 秒杀订单
-- ============================================================
CREATE TABLE `tb_voucher_order` (
    `id`         BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `order_no`   VARCHAR(32)     NOT NULL COMMENT '订单号',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `voucher_id` BIGINT UNSIGNED NOT NULL COMMENT '优惠券 ID',
    `shop_id`    BIGINT UNSIGNED NOT NULL COMMENT '店铺 ID',
    `amount`     BIGINT          NOT NULL COMMENT '支付金额(分)',
    `status`     VARCHAR(20)     NOT NULL DEFAULT 'CREATED' COMMENT '状态:CREATED / PAID / CANCELLED',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_voucher_user` (`voucher_id`, `user_id`),
    KEY `idx_user_id` (`user_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '秒杀订单';

-- ============================================================
-- 11. 笔记
-- ============================================================
CREATE TABLE `tb_blog` (
    `id`            BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '作者',
    `shop_id`       BIGINT UNSIGNED NULL COMMENT '关联店铺(可选)',
    `title`         VARCHAR(128)    NOT NULL COMMENT '标题',
    `content`       LONGTEXT        NOT NULL COMMENT '正文',
    `images`        JSON            NULL COMMENT '图片 URL 数组',
    `liked_count`   INT             NOT NULL DEFAULT 0 COMMENT '点赞数(冗余,便于排序)',
    `comment_count` INT             NOT NULL DEFAULT 0 COMMENT '评论数(冗余)',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态:PUBLISHED / HIDDEN / DELETED',
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_shop_id` (`shop_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '笔记';

-- ============================================================
-- 12. 收藏
-- ============================================================
CREATE TABLE `tb_blog_favorite` (
    `id`         BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `blog_id`    BIGINT UNSIGNED NOT NULL COMMENT '笔记 ID',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_user` (`blog_id`, `user_id`),
    KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '笔记收藏';

-- ============================================================
-- 13. 评论
-- ============================================================
CREATE TABLE `tb_blog_comment` (
    `id`               BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `blog_id`          BIGINT UNSIGNED NOT NULL COMMENT '笔记 ID',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '评论人',
    `parent_id`        BIGINT UNSIGNED NULL COMMENT '父评论 ID;NULL 表示一级评论',
    `reply_to_user_id` BIGINT UNSIGNED NULL COMMENT '被回复用户',
    `content`          VARCHAR(1000)   NOT NULL COMMENT '内容',
    `status`           VARCHAR(20)     NOT NULL DEFAULT 'VISIBLE' COMMENT '状态:VISIBLE / HIDDEN',
    `deleted`          TINYINT         NOT NULL DEFAULT 0,
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_blog_status` (`blog_id`, `status`, `created_at`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '笔记评论';

-- ============================================================
-- 14. 关注关系
-- ============================================================
CREATE TABLE `tb_follow` (
    `id`             BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '关注者',
    `follow_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注者',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`),
    KEY `idx_follow_user` (`follow_user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '关注关系';

-- ============================================================
-- 15. 签到审计
-- ============================================================
CREATE TABLE `tb_sign` (
    `id`         BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `sign_date`  DATE            NOT NULL COMMENT '签到日期',
    `year`       SMALLINT        NOT NULL COMMENT '年',
    `month`      TINYINT         NOT NULL COMMENT '月',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `sign_date`),
    KEY `idx_user_year_month` (`user_id`, `year`, `month`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '签到审计(Redis Bitmap 为准,本表用于统计与补偿)';

-- ============================================================
-- 16. 积分流水
-- ============================================================
CREATE TABLE `tb_point_log` (
    `id`            BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `change_amount` INT             NOT NULL COMMENT '变动值,正数增加,负数扣减',
    `balance`       INT             NOT NULL COMMENT '变动后余额',
    `source`        VARCHAR(32)     NOT NULL COMMENT '来源:SIGN_IN / SIGN_IN_STREAK / ORDER / ADMIN / EXCHANGE',
    `biz_id`        VARCHAR(64)     NULL COMMENT '业务 ID(用于幂等)',
    `remark`        VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '备注',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_source_biz` (`user_id`, `source`, `biz_id`),
    KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '积分流水(只增不改)';

-- ============================================================
-- 17. 通知
-- ============================================================
CREATE TABLE `tb_notification` (
    `id`         BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '接收人',
    `type`       VARCHAR(32)     NOT NULL COMMENT '类型:ORDER_CREATED / ORDER_ACCEPTED / SECKILL_RESULT / MERCHANT_AUDIT / SYSTEM_NOTICE',
    `title`      VARCHAR(128)    NOT NULL COMMENT '标题',
    `content`    VARCHAR(1000)   NOT NULL COMMENT '内容',
    `biz_id`     VARCHAR(64)     NULL COMMENT '关联业务 ID',
    `is_read`    TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已读',
    `read_at`    DATETIME        NULL COMMENT '已读时间',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read_created` (`user_id`, `is_read`, `created_at`),
    KEY `idx_biz_id` (`biz_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知';

-- ============================================================
-- 18. AI 会话
-- ============================================================
CREATE TABLE `tb_ai_conversation` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '所属用户',
    `scope`           VARCHAR(20)     NOT NULL COMMENT '范围:USER / MERCHANT',
    `title`           VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '会话标题',
    `summary`         TEXT            NULL COMMENT '历史对话摘要(Phase D)',
    `summary_message_id` BIGINT UNSIGNED NULL COMMENT '已摘要到的消息 ID',
    `last_message_at` DATETIME        NULL COMMENT '最后消息时间',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_scope_created` (`user_id`, `scope`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 会话';

-- ============================================================
-- 19. AI 消息
-- ============================================================
CREATE TABLE `tb_ai_message` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `conversation_id` BIGINT UNSIGNED NOT NULL COMMENT '会话 ID',
    `role`            VARCHAR(20)     NOT NULL COMMENT '角色:USER / ASSISTANT / TOOL',
    `content`         TEXT            NOT NULL COMMENT '消息内容',
    `tool_calls`      JSON            NULL COMMENT '工具调用记录',
    `tokens_in`       INT             NOT NULL DEFAULT 0 COMMENT '输入 token 数',
    `tokens_out`      INT             NOT NULL DEFAULT 0 COMMENT '输出 token 数',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_created` (`conversation_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 消息';

-- ============================================================
-- 20. 本地消息表(Outbox)
-- ============================================================
CREATE TABLE `tb_outbox` (
    `id`            BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `message_id`    VARCHAR(64)     NOT NULL COMMENT '消息唯一 ID(UUID)',
    `topic`         VARCHAR(128)    NOT NULL COMMENT 'Kafka topic',
    `biz_type`      VARCHAR(32)     NOT NULL COMMENT '业务类型',
    `biz_id`        VARCHAR(64)     NOT NULL COMMENT '业务 ID',
    `payload`       JSON            NOT NULL COMMENT '消息体',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态:PENDING / SENT / ACKED / FAILED',
    `retry_count`   INT             NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_at` DATETIME        NULL COMMENT '下次重试时间',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_status_retry` (`status`, `next_retry_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '本地消息表';

-- ============================================================
-- 21. 对账日志
-- ============================================================
CREATE TABLE `tb_reconcile_log` (
    `id`           BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `biz_type`     VARCHAR(32)     NOT NULL COMMENT '业务类型:SECKILL_STOCK / ORDER',
    `biz_id`       VARCHAR(64)     NOT NULL COMMENT '业务 ID',
    `redis_value`  VARCHAR(255)    NOT NULL DEFAULT '' COMMENT 'Redis 侧值',
    `db_value`     VARCHAR(255)    NOT NULL DEFAULT '' COMMENT 'DB 侧值',
    `diff`         BIGINT          NOT NULL DEFAULT 0 COMMENT '差异值',
    `status`       VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态:PENDING / RESOLVED / IGNORED',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz` (`biz_type`, `biz_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '对账日志';

-- ============================================================
-- 22. 幂等键
-- ============================================================
CREATE TABLE `tb_idempotency` (
    `id`                BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `idem_key`          VARCHAR(64)     NOT NULL COMMENT '客户端 Idempotency-Key',
    `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `biz_type`          VARCHAR(32)     NOT NULL COMMENT '业务类型:ORDER / SECKILL_ORDER',
    `biz_id`            VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '首次执行产生的业务 ID',
    `response_snapshot` JSON            NULL COMMENT '首次响应快照',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING' COMMENT '状态:PROCESSING / SUCCESS / FAILED',
    `expire_at`         DATETIME        NOT NULL COMMENT '过期时间',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_key` (`user_id`, `idem_key`),
    KEY `idx_expire_at` (`expire_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '幂等键';

-- ============================================================
-- 23. AI 知识库分片
-- ============================================================
CREATE TABLE `tb_knowledge_chunk` (
    `id`           BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `doc_id`       VARCHAR(64)     NOT NULL COMMENT '文档 ID',
    `scope`        VARCHAR(20)     NOT NULL DEFAULT 'ALL' COMMENT '适用端:USER / MERCHANT / ALL',
    `title`        VARCHAR(255)    NOT NULL COMMENT '文档标题',
    `section`      VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '章节标题',
    `content`      TEXT            NOT NULL COMMENT '分片正文',
    `embedding`    JSON            NOT NULL COMMENT '向量(embedding-3,1024 维)',
    `dimension`    INT             NOT NULL COMMENT '向量维度',
    `content_hash` CHAR(64)        NOT NULL COMMENT '分片内容哈希',
    `enabled`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_hash` (`doc_id`, `content_hash`),
    KEY `idx_scope_enabled` (`scope`, `enabled`),
    FULLTEXT KEY `ft_title_content` (`title`, `content`) WITH PARSER ngram
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 知识库分片';

-- ============================================================
-- 24. 店铺日统计(订单事件异步聚合)
-- ============================================================
CREATE TABLE `tb_shop_daily_stats` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `shop_id`         BIGINT UNSIGNED NOT NULL COMMENT '店铺 ID',
    `stat_date`       DATE            NOT NULL COMMENT '统计日期',
    `order_count`     BIGINT          NOT NULL DEFAULT 0 COMMENT '下单数',
    `paid_count`      BIGINT          NOT NULL DEFAULT 0 COMMENT '支付数',
    `completed_count` BIGINT          NOT NULL DEFAULT 0 COMMENT '完成数',
    `cancel_count`    BIGINT          NOT NULL DEFAULT 0 COMMENT '取消数',
    `close_count`     BIGINT          NOT NULL DEFAULT 0 COMMENT '超时关闭数',
    `gmv`             BIGINT          NOT NULL DEFAULT 0 COMMENT '成交额(分)',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shop_date` (`shop_id`, `stat_date`),
    KEY `idx_date` (`stat_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '店铺日统计';

-- ============================================================
-- 25. 订单事件消费去重
-- ============================================================
CREATE TABLE `tb_order_event_log` (
    `id`         BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `event_id`   VARCHAR(64)     NOT NULL COMMENT '事件 ID(Outbox messageId)',
    `event_type` VARCHAR(32)     NOT NULL COMMENT '事件类型',
    `order_id`   BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    `shop_id`    BIGINT UNSIGNED NOT NULL COMMENT '店铺 ID',
    `stat_date`  DATE            NOT NULL COMMENT '归属统计日期',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_order` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单事件消费去重';

SET FOREIGN_KEY_CHECKS = 1;
