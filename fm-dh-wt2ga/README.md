# fm-dh-wt2ga

> 大华 DH-HY-WT2GA 独立式感温探测器采集器：接收天翼物联网平台（CTWing）推送，转成统一信封后只投递 RabbitMQ，不写业务库。

## 📋 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [本地联调](#本地联调)
- [配置说明](#配置说明)
- [部署指南](#部署指南)

## 项目简介

本模块是消防监控平台重构后的第一个采集器实现，用于替换旧工程 `nsp-wt2ga` 的直连数据库写法。上行链路固定为「CTWing 推送 → 本地注册表校验 → 统一信封 → RabbitMQ」，入库与报警闭环全部由 `fm-api` 负责。

包按「改一处要开哪几个文件」划分：`contract/` 随 MQ 契约变，`ctwing/` 随平台报文变，`runtime/` 随设备量级变，`config/` 只随部署参数变。

### 核心功能

- [x] 接收 CTWing 推送并异步分发，保持 200 响应
- [x] 按 `sn` / `aepId` 双索引解析注册设备
- [x] `eventReport` 映射为 `ALARM` / `RECOVER` 信封
- [x] `dataReport` 按 sn 合并为 `REALTIME` 快照
- [x] 上线通知转 `HEARTBEAT`，下线只记日志
- [x] 确定性 `messageId` 保证平台重推幂等

## 技术栈

| 分类 | 技术 | 版本 |
| ---- | ---- | ---- |
| 运行时 | JDK | 1.8 |
| 框架 | Spring Boot | 2.7.18 |
| 消息 | Spring AMQP / RabbitMQ | 2.4.x / 3.x |
| 缓存 | Spring Data Redis | 2.7.x |
| 构建 | Maven | 3.9.8 |
| 测试 | JUnit 5 + Mockito | Spring Boot 2.7 自带 |

## 项目结构

```text
fm-dh-wt2ga/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/fm/dh/wt2ga/
    │   │   ├── Wt2gaApplication.java   # 启动入口
    │   │   ├── contract/               # 信封、幂等 ID、MQ 发布、交换机拓扑
    │   │   ├── ctwing/                 # 报文 DTO、事件映射、快照合并、推送入口
    │   │   ├── runtime/                # 设备注册表、处理线程池
    │   │   └── config/                 # 配置绑定
    │   └── resources/
    │       ├── application.yml
    │       ├── logback-spring.xml
    │       └── thing-model.json
    └── test/
        ├── java/com/fm/dh/wt2ga/       # 单元测试与回放测试，包结构与主代码一致
        └── resources/fixtures/         # 线上真实报文
```

## 环境要求

- JDK 1.8
- Maven 3.9+
- Redis 6.x 及以上（读取 `fm:device:*` 注册表）
- RabbitMQ 3.x（投递 `fm.device.up` / `fm.device.realtime`）

## 快速开始

```bash
# 编译并运行测试
mvn -pl fm-dh-wt2ga -am test

# 打包
mvn -pl fm-dh-wt2ga -am package -DskipTests

# 启动（默认监听 9092，context-path 为 /fm-dh-wt2ga）
java -jar fm-dh-wt2ga/target/fm-dh-wt2ga-1.0.0-SNAPSHOT.jar
```

## 本地联调

用一台已注册设备走通「CTWing 推送 → MQ → fm-api 入库」的完整链路。

### 启动顺序

1. 确认 MySQL / Redis / RabbitMQ 可达（默认 `192.168.1.20`）。
2. 启动 `fm-api`：`java -jar fm-api/target/fm-api-1.0.0-SNAPSHOT.jar`，它负责声明队列与消费入库。
3. 启动采集器：`java -jar fm-dh-wt2ga/target/fm-dh-wt2ga-1.0.0-SNAPSHOT.jar`。
4. 采集器日志出现 `device registry refreshed, sn=N, aep=M` 才算注册表加载成功；`aep` 为 0 表示设备哈希里还没有 `aepId`。

### 回调地址

```text
http://<采集器地址>:9092/fm-dh-wt2ga/callback
```

### 验证步骤

按「上线 → 参数上报 → 温度报警 → 重复一次报警 → 温度恢复」顺序 POST 报文，然后核对：

```sql
SELECT sn, alarm_type, alarm_level, event_code, alarm_time, recover_time
FROM b_fire_alarm WHERE sn = '<sn>' ORDER BY id DESC LIMIT 3;
SELECT sn, type, data, report_time FROM b_device_realtime WHERE sn = '<sn>';
SELECT sn, online_status, last_time FROM b_device WHERE sn = '<sn>';
```

预期：报警只有 1 条（重复推送被 `message_id` 唯一索引吞掉）、`recover_time` 已回填、`b_fire_alarm_trace` 有「设备上报」与「设备恢复」两条、快照同时含温度与电池字段、`online_status=1`。

### 两个真实约束

- **选设备要避开存量未恢复报警。** `AlarmServiceImpl.isDuplicateAlarm` 按 `sn + alarmType + 未恢复` 判重，同一 SN 只要还有未恢复报警，新报警会被直接吞掉，看起来像链路断了。
- **回放过旧时间戳不会刷新在线状态。** `DeviceStatusServiceImpl` 有乱序保护，心跳时间早于 `b_device.last_time` 时只更新 Redis 注册表，不更新 `online_status`。

## 配置说明

| 配置项 | 说明 | 默认值 |
| ---- | ---- | ---- |
| `server.port` | 推送回调端口，环境变量 `CTWING_PUSH_PORT` | `9092` |
| `server.servlet.context-path` | 应用根路径，环境变量 `CTWING_PUSH_CONTEXT` | `/fm-dh-wt2ga` |
| `ctwing.push.callback-path` | CTWing 推送回调路径 | `/callback` |
| `spring.redis.host` / `spring.redis.port` | Redis 地址，环境变量 `REDIS_HOST` / `REDIS_PORT` | `192.168.1.20` / `6379` |
| `spring.rabbitmq.host` / `spring.rabbitmq.port` | RabbitMQ 地址，环境变量 `RABBITMQ_HOST` / `RABBITMQ_PORT` | `192.168.1.20` / `5672` |
| `fm.registry.refresh-interval-ms` | 设备注册表刷新间隔，环境变量 `FM_REGISTRY_REFRESH_MS` | `60000` |
| `fm.registry.device-type` | 只加载该设备类型的注册条目 | `dh-wt2ga` |
| `fm.publish.retry-delays-ms` | 发布失败的重试间隔（毫秒，逗号分隔），重试次数等于元素个数 | `1000,2000,4000` |
| `fm.raw-log-enabled` | 是否打印原始推送报文，环境变量 `FM_RAW_LOG` | `true` |

## 部署指南

```bash
mvn -pl fm-dh-wt2ga -am package -DskipTests
java -jar fm-dh-wt2ga/target/fm-dh-wt2ga-1.0.0-SNAPSHOT.jar
```

- 默认监听 `9092`，context-path 为 `/fm-dh-wt2ga`，回调整体路径：`http://<host>:9092/fm-dh-wt2ga/callback`。
- 端口与根路径分别用 `CTWING_PUSH_PORT`、`CTWING_PUSH_CONTEXT` 覆盖；改 context-path 后 CTWing 控制台的订阅地址要同步改。
- 日志落在 `logs/fm-dh-wt2ga.log`，按天 + 50MB 滚动，保留 14 天。
- 部署前确认 Redis 的 `fm:device:all` 与 `fm:device:{sn}`（含 `aepId`）已由 fm-api 写入，否则推送会被当作未注册设备丢弃（日志 `unregistered device`）。

需要在 CTWing 控制台配置的订阅地址（由部署方填写，本仓库不包含平台侧操作）：

```text
http://<采集器外网地址>:9092/fm-dh-wt2ga/callback
```

CTWing 不提供签名或 token，应用层没有可校验的凭据，因此代码不做来源校验；暴露面控制放在反向代理或 IP 白名单，由运维评估后实施。
