# 秒杀压测指南(JMeter)

## 1. 为什么不能直接用 5 个用户测

5 个用户只能验证功能,验证不了:

- 超卖
- 重复订单
- 限流是否生效
- Redis / DB / Outbox 在压力下的表现
- 接口 P95 / 吞吐量

高并发测试需要:**足够多的独立用户 + 同一个秒杀券**。

---

## 2. 准备 1000 个用户和 JWT

不能让 JMeter 走短信登录(有 60 秒限流,而且很慢)。

项目里提供了一个数据生成器:

`src/test/java/com/laotang/core/loadtest/LoadTestDataGeneratorTest.java`

### 执行

在项目根目录:

```bash
mvn test -Dtest=LoadTestDataGeneratorTest -Dloadtest.generate=true
```

执行完成后生成:

```
clp-core-service/target/loadtest-users.csv
```

格式:

```
userId,phone,token
3697...,13900000000,eyJhbGciOiJIUzM4NCJ9...
```

- 1000 个用户
- 手机号 `13900000000` ~ `13900000999`
- 每个用户一个有效 JWT

> 该测试默认不参与 `mvn test`,只在带 `-Dloadtest.generate=true` 时运行。

---

## 3. 准备秒杀券

用商户 token 创建一张库存 **100** 的秒杀券(故意小于 1000,才能测出"恰好卖完"):

```bash
curl -X POST http://localhost:8080/api/v1/vouchers \
  -H "Authorization: Bearer <商户 token>" \
  -H "Content-Type: application/json" \
  -d '{
    "shopId": 你的店铺ID,
    "title": "压测券",
    "payValue": 1000,
    "actualValue": 2000,
    "stock": 100,
    "perUserLimit": 1,
    "beginTime": "2026-10-17T00:00:00",
    "endTime": "2026-12-31T23:59:59"
  }'
```

记下返回的券 ID。

---

## 4. 调大服务端容量(重要)

默认配置是给日常开发用的,压测前临时调大:

### 4.1 启动参数

```bash
java -jar clp-core-service/target/clp-core-service-1.0-SNAPSHOT.jar \
  --server.port=8080 \
  --server.tomcat.threads.max=500 \
  --server.tomcat.accept-count=1000 \
  --spring.datasource.hikari.maximum-pool-size=50 \
  --spring.data.redis.lettuce.pool.max-active=64 \
  --mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl \
  --logging.level.com.laotang=warn
```

| 参数 | 作用 |
|---|---|
| `tomcat.threads.max` | 处理请求的线程数 |
| `tomcat.accept-count` | 等待队列长度 |
| `hikari.maximum-pool-size` | 数据库连接池 |
| `lettuce.pool.max-active` | Redis 连接池 |
| `log-impl=NoLoggingImpl` | **关掉 SQL 打印**,否则 IO 会拖垮压测 |
| `logging.level.com.laotang=warn` | 降低业务日志量 |

---

## 5. 运行 JMeter

### 5.1 导入计划

打开 JMeter → `File → Open` → 选择:

```
community-life-platform/loadtest/seckill.jmx
```

### 5.2 修改变量

在 **Test Plan → User Defined Variables** 中改:

| 变量 | 值 |
|---|---|
| `host` | `localhost` |
| `port` | `8080` |
| `voucherId` | 上一步创建的券 ID |
| `csvFile` | **CSV 的绝对路径** |

`csvFile` 示例:

```
C:/Users/whatcanisayman/Desktop/面向社区用户的生活服务平台/community-life-platform/clp-core-service/target/loadtest-users.csv
```

### 5.3 先用小并发验证

把 Thread Group 的线程数改成 **10**,跑一遍,确认:

- 令牌接口返回 `code=0`
- 秒杀接口返回 `code=0`

### 5.4 正式压测(命令行,不要用 GUI)

GUI 本身会消耗大量资源,正式压测用:

```bash
jmeter -n -t loadtest/seckill.jmx -l result.jtl -e -o report
```

参数:

| 参数 | 作用 |
|---|---|
| `-n` | 非 GUI 模式 |
| `-t` | 测试计划 |
| `-l` | 原始结果 |
| `-e -o report` | 生成 HTML 报告 |

报告位置:`report/index.html`。

### 5.5 JMeter 内存

打开 `jmeter.bat`,把:

```
set HEAP=-Xms1g -Xmx1g
```

改成:

```
set HEAP=-Xms2g -Xmx4g
```

1000 线程建议至少 2G 堆。

---

## 6. 压测后必须检查的数据

### 6.1 Redis

```bash
MSYS_NO_PATHCONV=1 docker exec redis redis-cli -a 879269 get "clp:seckill:stock:{voucherId}"
```

期望:`0`,**绝不能是负数**。

### 6.2 MySQL

```sql
-- 订单数应等于券库存
SELECT COUNT(*) FROM clp.tb_voucher_order WHERE voucher_id = {voucherId};

-- 不能有重复用户
SELECT COUNT(*), COUNT(DISTINCT user_id)
FROM clp.tb_voucher_order WHERE voucher_id = {voucherId};

-- DB 秒杀库存应为 0
SELECT stock FROM clp.tb_seckill_voucher WHERE voucher_id = {voucherId};

-- Outbox 应全部 ACKED
SELECT status, COUNT(*) FROM clp.tb_outbox GROUP BY status;
```

**验收标准:**

| 检查项 | 期望 |
|---|---|
| Redis 库存 | `0`,不为负 |
| 订单数 | `100`(等于券库存) |
| 去重用户数 | `100` |
| DB 秒杀库存 | `0` |
| Outbox | 成功订单数条 `ACKED` |
| 重复订单 | 无 |

### 6.3 JMeter 报告

看这几项:

- **Throughput**:每秒请求数
- **95% / 99% Line**:P95 / P99 响应时间
- **Error %**:会包含预期内的 `7003`(库存不足),不要当成 bug

秒杀接口的返回分布应该是:

- `202 + code=0`:100 次左右
- `409 + code=7003`:其余大多数

所以**错误率不是 0 是正常的**,关键看订单数是否正好等于库存。

---

## 7. 本地压测的边界

| 限制 | 说明 |
|---|---|
| JMeter 和应用同机 | CPU/内存互相抢占,结果偏保守 |
| Windows 临时端口 | 1000 并发没问题,上万需要调优 |
| Tomcat 线程 | 默认 200,不调大会排队 |
| MySQL 连接池 | 默认 10,不调大会成为瓶颈 |
| 日志 | 必须关闭 SQL 日志,否则测的是磁盘 IO |
| Redis 单机 | 本地单机 Redis,压测上限有限 |

**结论:** 本地 1000 并发验证"正确性"足够;要测极限 QPS,建议 JMeter 和应用分两台机器,或用云压测。

---

## 8. 建议的压测阶梯

不要一上来就 1000:

```
10  →  50  →  200  →  500  →  1000
```

每一档记录:

- 吞吐量
- P95 / P99
- 订单数是否仍等于库存
- Redis 库存是否仍 >= 0
- Outbox 是否最终全部 ACKED

这样能看出瓶颈出现在哪一档。
