# HeartBeat — 领域驱动设计（DDD）Spring Boot 工程脚手架

基于 **经典四层 DDD 架构** 的 Spring Boot 多模块工程，技术栈 Spring Boot 2.7.x + Java 8 + MyBatis + MyBatis Generator + MySQL。
以最小的 `User` 聚合作为示例，重点演示分层骨架、依赖方向与各层职责。

## 技术栈

| 组件 | 版本 |
| --- | --- |
| Spring Boot | 2.7.18 |
| Java | 8 |
| 持久化 | MyBatis + MyBatis Generator |
| 数据库 | MySQL 8.0（生产） / H2（本地） |
| 构建 | Maven 多模块 |
| 工具 | Lombok、MapStruct |

## 模块与分层

```
heartbeat (父 POM，统一依赖与版本管理)
├── heartbeat-domain          领域层：聚合根 / 实体 / 值对象 / 领域事件 / 领域服务 / 仓储接口
├── heartbeat-application     应用层：用例编排 / 命令 / 查询 / DTO / 装配器 / 事务边界
├── heartbeat-infrastructure  基础设施层：仓储实现(MyBatis + MyBatis Generator) / PO / 转换器 / ID 生成 / 事件发布
├── heartbeat-interfaces      用户接口层：REST 控制器 / 请求响应 VO / 参数校验 / 统一返回 / 全局异常
└── heartbeat-start           启动模块：主类 + 配置文件，装配各层、唯一可执行入口
```

### 依赖方向（核心原则）

```
interfaces ──▶ application ──▶ domain ◀── infrastructure
                                  ▲              │
                                  └──── 依赖倒置 ─┘
        start ──▶ interfaces + infrastructure（运行期装配）
```

- **domain 是核心，不依赖任何其它层**，保持纯净（无框架注解，纯 POJO）。
- **依赖倒置**：仓储接口 `UserRepository` 定义在领域层，实现 `UserRepositoryImpl` 在基础设施层。领域层只认抽象。
- `application` 只依赖 `domain`；`infrastructure` 只依赖 `domain`；`interfaces` 只依赖 `application`。
- `start` 在运行期把 `interfaces` 与 `infrastructure` 聚合到一起，完成 Spring 容器装配。

### 各层职责速查

| 层 | 该写什么 | 不该写什么 |
| --- | --- | --- |
| domain | 业务规则、不变量、状态流转、领域事件 | 框架注解、SQL、HTTP |
| application | 用例编排、事务、调用领域对象、发布事件、转 DTO | 业务规则（应下沉到 domain） |
| infrastructure | ORM 映射、仓储实现、外部集成、缓存、MQ | 业务规则 |
| interfaces | 协议适配、参数校验、统一返回、异常翻译 | 业务逻辑 |

## 关键设计点

- **充血模型**：`User` 聚合根无公开 setter，所有变更通过行为方法（`register` / `changeEmail` / `disable`）进行，规则内聚。
- **值对象**：`Email` 在构造时校验格式并归一化（小写），使"非法邮箱无法被表达"。
- **领域服务**：`UserRegistrationService` 承载跨聚合的规则（邮箱全局唯一）。
- **领域事件**：`UserRegisteredEvent` 由聚合登记，应用层在事务内发布；`SpringDomainEventPublisher` 为端口的适配实现，后续可平滑替换为 MQ/Outbox。
- **应用分配标识**：`SnowflakeIdGenerator` 在持久化前即生成聚合 ID，避免依赖数据库自增列。
- **模型隔离**：领域模型 `User`、应用 DTO `UserDTO`、持久化对象 `UserPO`、接口响应 `UserResponse` 四者分离，各层可独立演进。

## 快速开始

### 本地运行（H2 内存库，零外部依赖）

```bash
mvn clean package -DskipTests
java -jar heartbeat-start/target/heartbeat.jar --spring.profiles.active=local
```

默认激活 `dev` profile；本地零依赖运行时显式启用 `local`，使用 H2 内存库并自动建表。
启动后访问 `http://localhost:7001`，H2 控制台为 `http://localhost:7001/h2-console`（JDBC URL：`jdbc:h2:mem:heartbeat`）。

### 可选中间件开关

Redis、RocketMQ、Kafka、MQTT、Quartz 默认全部关闭：

```properties
REDIS_ENABLED=false
ROCKETMQ_ENABLED=false
KAFKA_ENABLED=false
MQTT_ENABLED=false
QUARTZ_ENABLED=false
```

未开启时应用不会创建对应客户端、监听器或 Quartz 调度器，也不会因为本机没有这些外部服务而启动失败。每个 profile 支持自己的地址变量，例如 `DEV_ROCKETMQ_NAME_SERVER`、`TEST_KAFKA_BOOTSTRAP_SERVERS`、`PROD_REDIS_HOST`；未配置 profile 专属变量时回退到通用变量。

RabbitMQ 已替换为 RocketMQ。开启 RocketMQ 时至少配置：

```properties
ROCKETMQ_ENABLED=true
ROCKETMQ_NAME_SERVER=rocketmq-namesrv:9876
ROCKETMQ_PRODUCER_GROUP=heartbeat-prod
```

### MySQL 数据库初始化

各环境默认数据库名：

| Profile | 数据库 |
| --- | --- |
| `dev` | `heartbeat_dev` |
| `test` | `heartbeat_test` |
| `pre` | `heartbeat_pre` |
| `gray` | `heartbeat_gray` |
| `prod` | `heartbeat` |

SQL 维护源位于 `heartbeat-start/src/main/resources/db/mysql/`：

- `00-create-databases.sql`：创建五套环境数据库。
- `db/migration/mysql/V1`～`V8`：Flyway 正式数据库契约。
- `heartbeat-enterprise-all.sql`：由 Flyway 迁移聚合的一键安装脚本，不包含建库语句。

Windows PowerShell 下可这样初始化：

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -u root -p < heartbeat-start\src\main\resources\db\mysql\00-create-databases.sql"
cmd /c "mysql --default-character-set=utf8mb4 -u root -p heartbeat_dev < heartbeat-start\src\main\resources\db\mysql\heartbeat-enterprise-all.sql"
```

修改模块脚本后，使用以下命令重新生成完整脚本及旧兼容入口：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File heartbeat-start\src\main\resources\db\build-sql-bundles.ps1
```

`pre`、`gray`、`prod` 使用 Quartz JDBC JobStore，需要安装 `QRTZ_*` 表；完整脚本已包含这些表。`local`、`dev`、`test` 默认使用内存 JobStore。

### 生产运行（MySQL）

```bash
java -jar heartbeat-start/target/heartbeat.jar \
  --spring.profiles.active=prod \
  --MYSQL_HOST=127.0.0.1 --MYSQL_DB=heartbeat \
  --MYSQL_USERNAME=root --MYSQL_PASSWORD=yourpwd
```

生产环境不会自动建表，请先执行 `db/mysql/heartbeat-enterprise-all.sql`，或交给 Flyway 执行 `db/migration/mysql`。

默认管理员账号为 `admin/admin123`，仅用于首次初始化，登录后必须立即修改密码。正式环境不得在 SQL 中写入支付、OAuth、数据库或消息中间件真实密钥。

## REST API 示例

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/users` | 注册用户 |
| GET | `/api/v1/users/{id}` | 查询用户详情 |
| PUT | `/api/v1/users/{id}/email` | 修改邮箱 |
| DELETE | `/api/v1/users/{id}` | 停用用户 |

```bash
# 注册
curl -X POST http://localhost:7001/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com"}'
```

统一返回结构：`{"code":"0","msg":"success","data":{...}}`，失败时 `code` 为稳定业务错误码（如 `USER_EMAIL_DUPLICATED`）。

## 关于本机 Maven 镜像（已修复）

此前全局 `settings.xml` 的 `fanruan` 镜像（`mirrorOf=*`，`mvn.finedevelop.com`）域名已失效、返回 HTML 错误页，
污染了本地仓库导致构建失败。现已修复：

- 移除失效的 `fanruan` 镜像与对应的 `fanruan` profile / activeProfile；
- 主镜像改为阿里云公共仓库（`https://maven.aliyun.com/repository/public`），并在配置中预留腾讯云、华为云、网易、官方中央仓库作为可切换的备用源；
- 清理了本地仓库中 469 个被污染的构件与 28 个 fanruan 残留元数据。

因此现在直接用标准命令即可构建，无需任何额外参数：

```bash
mvn clean package -DskipTests
```

## 扩展指引

- **新增聚合**：在 `domain/<context>` 下建模聚合根/值对象/仓储接口/领域事件 → 应用层加用例服务 → 基础设施层实现仓储 → 接口层加 Controller。
- **异步事件**：将 `SpringDomainEventPublisher` 替换为基于 Outbox 表 + MQ 的实现，上层无需改动。
- **读写分离 / CQRS**：查询可绕过聚合，直接在应用层定义独立的查询服务与读模型。
```
