# 面向社区用户的生活服务平台

> Java 后端实习主项目 · Spring Boot 3.5 + JDK 21 · 单体多模块

## 一句话

面向社区的三方生活服务平台:**用户发内容、下订单,商户接单、看经营,管理员审核、看数据**。

三条技术主线:

- **高并发**:优惠券秒杀 —— Redis + Lua 原子扣减、令牌、一人一单、Kafka 异步落库、Outbox、对账
- **AI 应用**:Spring AI 客服 —— 提示词工程、Function Calling、RAG、会话记忆、超时/熔断/降级、RAG 评估
- **数据链路**:订单事件 → Outbox → Kafka → 店铺日统计 → 商户报表 / 管理看板

## 项目亮点

| 亮点 | 说明 |
|---|---|
| 秒杀一致性 | Redis Lua 原子完成令牌校验、一人一单、库存扣减;Outbox + Kafka 最终一致;定时对账兜底 |
| 订单可靠性 | `Idempotency-Key` 幂等、条件扣库存、状态机、Redisson 延时关单 + 定时扫表兜底 |
| Feed 推拉结合 | 普通作者写粉丝收件箱,大 V 只写发件箱,读取时合并去重、游标分页 |
| AI 工具调用 | 10 个只读工具,`userId/shopId` 由服务端注入,模型无法越权 |
| RAG | 向量 + ngram 全文双路召回、RRF 融合、rerank-pro 精排、证据阈值过滤、来源返回 |
| AI 可靠性 | 会话摘要记忆、Redisson 会话锁、15s 硬超时、熔断、固定话术降级、HitRate/MRR 评估 |
| 数据链路 | 订单事务内写 Outbox,事件幂等消费,聚合日统计,支撑商户报表与管理看板 |
| 多级缓存 | Caffeine L1 + Redis L2,更新与库存变化时两级失效 |
| 管理看板 | 单页 Dashboard,管理员登录后查看 KPI 与近 7 天趋势,无外部前端依赖 |

## 已实现模块

| 模块 | 能力 |
|---|---|
| 认证 | 手机号验证码登录、首次注册、JWT access/refresh、刷新轮换、登出、RBAC、TraceId |
| 用户 | 资料查看/修改、关注/取关、粉丝/关注列表 |
| 商户 | 入驻申请、管理员审核、通过后建店并升级角色 |
| 店铺 | 列表/详情/分类、商户修改自己的店铺、多级缓存 |
| 商品 | 新增、在售列表、商户管理列表、详情、修改、上下架、逻辑删除、乐观锁 |
| 订单 | 幂等下单、条件扣库存、状态机、取消回滚、商户接单/完成、延时关单 |
| 营销 | 优惠券创建、秒杀令牌、Redis+Lua、一人一单、Kafka 异步落库、消费幂等、对账、限流 |
| 内容社区 | 笔记发布/详情/删除、点赞、收藏、评论回复、热门、管理员隐藏、推拉结合 Feed |
| 成长 | Bitmap 签到、每日积分、连续 7 天奖励、积分流水 |
| 通知 | WebSocket 握手鉴权、Redis Pub/Sub 跨节点、离线落库、已读/未读数 |
| AI 客服 | 会话、提示词、工具调用、RAG、摘要记忆、会话锁、超时/熔断/降级、RAG 评估 |
| 经营统计 | 订单事件、Kafka 消费、店铺日统计、商户经营报表、管理端数据看板与只读页面 |

## 技术栈

| 层 | 选型 |
|---|---|
| 基础 | JDK 21 · Spring Boot 3.5.4 · Maven 多模块 |
| 数据 | MySQL 8 · MyBatis-Plus 3.5.7 |
| 缓存 | Redis 7 · Redisson · Lua · Caffeine(Caffeine + Redis 两级缓存) |
| 消息 | Kafka 4.0(Outbox、手动 ACK、消费幂等) |
| 实时 | WebSocket · Redis Pub/Sub |
| AI | Spring AI 1.0.0 · DeepSeek `deepseek-v4-pro` · 智谱 `embedding-3` / `rerank-pro` |
| 安全 | 短信验证码登录 · JWT(jjwt 0.12.6) · 统一拦截器 · RBAC |
| 文档 | springdoc-openapi 2.8.0 · Knife4j |
| 测试 | JUnit 5 · Mockito · AssertJ · JaCoCo |

> 未实现 / 明确不做:微服务、K8s、分库分表、布隆过滤器、真实支付、退款流程、配送调度、前端页面。

## 架构

### 模块

```
community-life-platform/
├── clp-common/              Result、ErrorCode、RedisKey、UserContext、工具
├── clp-parameter/           请求 / 响应 DTO(纯契约)
├── clp-id-generator/        雪花 ID
├── clp-framework-redis/     Redis 限流、Caffeine + Redis 两级缓存
├── clp-framework-lock/      Redisson 分布式锁、延时队列
├── clp-framework-mq/        预留(Kafka 目前由 core 的 Outbox / 消费者直接使用)
├── clp-framework-ai/        智谱 embedding / rerank 客户端
├── clp-core-service/        业务主模块(启动类、全部业务域)
├── sql/                     建表脚本与演示数据
├── docs/                    设计文档
└── loadtest/                压测与冒烟脚本
```

依赖方向单向:

```
clp-core-service
  ├── clp-parameter
  ├── clp-framework-redis ──┐
  ├── clp-framework-lock    ├── clp-common
  ├── clp-framework-mq      │
  ├── clp-framework-ai ─────┘
  └── clp-id-generator
```

包组织按业务域划分:`auth`、`user`、`merchant`、`shop`、`product`、`order`、`marketing`、`content`、`growth`、`notification`、`ai`、`stats`、`outbox`、`idempotency`、`reconcile`。

### 关键链路

| 链路 | 文档 |
|---|---|
| 登录 / 下单 / 秒杀 / AI / 通知 | [05-核心链路时序](docs/05-核心链路时序.md) |
| 秒杀令牌、Lua、Outbox、Kafka、对账 | [09-秒杀链路详解](docs/09-秒杀链路详解.md) |
| AI 四阶段设计与实现 | [10-AI客服设计](docs/10-AI客服设计.md) |
| 订单事件与经营统计 | [11-订单事件与经营统计](docs/11-订单事件与经营统计.md) |

## 关键设计说明

### 1. 秒杀

- 令牌:Redis 60 秒一次性令牌,先校验活动时间与资格。
- 原子扣减:Lua 内完成令牌校验、一人一单、库存判断与扣减。
- 最终一致:扣减成功写 Outbox,Kafka 异步落订单,消费端唯一索引幂等。
- 兜底:定时对账 Redis 库存与 DB 订单数量,差异写 `tb_reconcile_log`。
- 压测:1000 线程、库存 100,最终 100 单 / 100 个不同用户 / 无负库存。

### 2. 订单

- `Idempotency-Key` + Redis 快路径 + DB 唯一索引兜底。
- 条件扣库存:`stock >= qty AND status = 'ON_SALE'` 一条 SQL。
- 状态机:`CREATED → PAID → ACCEPTED → COMPLETED`,取消/超时分支。
- 关单:Redisson 延时队列 + 定时扫表兜底。

### 3. AI 客服

| 阶段 | 内容 |
|---|---|
| A | 会话持久化、提示词资源化、DeepSeek 对话、Redis 限流 |
| B | 工具调用:订单、商品、店铺、积分、经营数据;身份服务端注入 |
| C | RAG:双路召回、RRF、rerank-pro、证据阈值、来源返回 |
| D | 摘要记忆、Redisson 会话锁、15s 超时、熔断、固定话术降级、HitRate/MRR 评估 |

### 4. 数据链路

```
订单事务 → Outbox(同事务) → Kafka clp-order-event → 消费者
→ tb_order_event_log(去重) + tb_shop_daily_stats(幂等累加)
→ 商户经营报表 / 管理看板
```

当天数字实时查订单表,历史趋势查聚合表。

### 5. 多级缓存

- L1:Caffeine,进程内,TTL 60 秒。
- L2:Redis,店铺 30 分钟、商品 60 秒。
- 读:L1 → L2 → DB,命中后逐级回填。
- 写:更新店铺/商品、订单扣减或回滚库存时,删除 L1 + L2。
- 一致性边界:多节点下其他节点的 L1 无法被删除,靠 60 秒本地 TTL 收敛。

## 快速开始

### 1. 环境

- JDK 21、Maven 3.9+
- MySQL 8、Redis 7、Kafka 4.0

### 2. 初始化数据库

```bash
mysql -u root -p -e "
CREATE DATABASE IF NOT EXISTS clp
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;"

# 25 张表
mysql --default-character-set=utf8mb4 -u root -p clp < sql/01-schema.sql

# 可选:管理员、商户、店铺、3 个商品
mysql --default-character-set=utf8mb4 -u root -p clp < sql/02-demo-data.sql
```

完整的环境准备、Kafka topic、演示流程和常见坑见
[12-学习与复现指南](docs/12-学习与复现指南.md)。

### 3. 本地配置

```bash
cp clp-core-service/src/main/resources/application-local.yml.example \
   clp-core-service/src/main/resources/application-local.yml
```

填写:

- MySQL / Redis 密码
- `clp.jwt.secret`(至少 32 字节)
- `spring.ai.openai.api-key`(DeepSeek)
- `clp.ai.zhipu.api-key`(智谱,格式 `{id}.{secret}`)

`application-local.yml` 已在 `.gitignore` 中,不会入库。

### 4. 启动 Kafka 与 topic

```bash
docker compose up -d

docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists --topic clp-seckill-order \
  --partitions 3 --replication-factor 1

docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists --topic clp-order-event \
  --partitions 3 --replication-factor 1
```

### 5. 启动应用

```bash
mvn -DskipTests package
java -jar clp-core-service/target/clp-core-service-1.0-SNAPSHOT.jar
# 默认 8080,可加 --server.port=8084
```

- Swagger / Knife4j:`http://localhost:8080/doc.html`
- 管理看板:`http://localhost:8080/admin/dashboard.html`(需要 ADMIN 账号)
- 健康检查:`http://localhost:8080/actuator/health`

### 6. 冒烟测试

```bash
APP_LOG=path/to/app.log bash loadtest/smoke-test.sh
```

覆盖登录、店铺/商品、下单支付、订单查询、通知等主链路。

## 测试与压测

| 项 | 结果 |
|---|---|
| 单元测试 | 161 个通过,0 失败,1 跳过 |
| 秒杀压测 | 1000 线程 / 库存 100:100 单、100 个不同用户、无超卖 |
| 秒杀优化 | 应用侧优化后整体 avg 138.5 → 63.5ms,P99 769 → 495ms |
| RAG 评估 | 用户端 10/10、商户端 6/6,HitRate 与 MRR 均为 1.0 |
| AI 真实调用 | DeepSeek 工具调用、RAG 回答、摘要记忆、熔断降级均已端到端验证 |

压测脚本与说明见 [loadtest/README.md](loadtest/README.md)。

## 文档索引

| 文档 | 内容 |
|---|---|
| [01-业务定义](docs/01-业务定义.md) | 角色、业务域、核心规则、状态机、边界 |
| [02-接口规范](docs/02-接口规范.md) | URL、响应体、分页、鉴权、幂等、错误码 |
| [03-接口清单](docs/03-接口清单.md) | 全部端点按业务域列出 |
| [04-数据模型](docs/04-数据模型.md) | 25 张表 DDL、索引、Redis Key |
| [05-核心链路时序](docs/05-核心链路时序.md) | 登录、下单、秒杀、AI、通知 |
| [06-模块与依赖](docs/06-模块与依赖.md) | 模块划分、依赖方向、包结构 |
| [07-工程搭建](docs/07-工程搭建.md) | 模块、POM、包结构、启动配置 |
| [08-认证域设计](docs/08-认证域设计.md) | 验证码登录、JWT、刷新登出、拦截器 |
| [09-秒杀链路详解](docs/09-秒杀链路详解.md) | 令牌、Lua、Outbox、Kafka、幂等、对账 |
| [10-AI客服设计](docs/10-AI客服设计.md) | 对话、工具、RAG、记忆、可靠性、评估 |
| [11-订单事件与经营统计](docs/11-订单事件与经营统计.md) | 事件、幂等、日统计、报表、看板 |
| [12-学习与复现指南](docs/12-学习与复现指南.md) | 环境、SQL、Redis、Kafka、启动、演示、常见坑 |

## 已知边界

- 支付为模拟支付;退款、配送、积分兑换、线上结算未实现。
- 除只读管理看板外没有其他前端页面,业务接口通过 Swagger 调试。
- 尚未 Docker 化与 CI;Kafka 已提供 `docker-compose.yml`。
- 未使用布隆过滤器;缓存穿透暂未做专门处理。
- 日统计从功能上线开始累积,历史订单未回填。
- 多级缓存的 L1 为进程内缓存,多节点场景存在最长 60 秒的脏读窗口。
- AI 摘要同步生成,触发摘要的那次请求会多一次模型调用。

## License

仅用于学习与求职展示。
