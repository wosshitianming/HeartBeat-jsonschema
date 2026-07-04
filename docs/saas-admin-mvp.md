# SaaS 管理后台 MVP 实现说明

## 第一阶段范围

本阶段先落地管理后台底座的可运行闭环：

- 认证接口：账号密码登录、刷新令牌、退出登录、当前用户、第三方登录配置和回调扩展点。
- IAM 接口：菜单管理 CRUD、动态路由树、角色授权树、权限码下发。
- 后台资源 CRUD：租户、用户、部门、岗位、角色、字典、参数、通知、日志、任务、OAuth 客户端、第三方登录提供方。
- 动态菜单：左侧菜单由 `sys_menu` 驱动，支持目录、菜单、按钮、隐藏菜单和外链菜单。
- MySQL 表结构：正式维护源位于 `heartbeat-start/src/main/resources/db/migration/mysql/`，完整安装脚本为 `db/mysql/heartbeat-enterprise-all.sql`。

## 核心接口

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/iam/routes`
- `GET /api/v1/iam/menus`
- `POST /api/v1/iam/menus`
- `PUT /api/v1/iam/menus/{id}`
- `DELETE /api/v1/iam/menus/{id}`
- `GET /api/v1/admin/resources/{resource}`
- `POST /api/v1/admin/resources/{resource}`
- `PUT /api/v1/admin/resources/{resource}/{id}`
- `DELETE /api/v1/admin/resources/{resource}/{id}`

## MySQL 使用

环境与默认数据库对应关系：

| Profile | 数据库 | Quartz |
| --- | --- | --- |
| `dev` | `heartbeat_dev` | 内存 JobStore |
| `test` | `heartbeat_test` | 内存 JobStore |
| `pre` | `heartbeat_pre` | JDBC JobStore |
| `gray` | `heartbeat_gray` | JDBC JobStore |
| `prod` | `heartbeat` | JDBC JobStore |

首次部署先创建数据库，再向目标库安装完整结构：

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -u root -p < heartbeat-start\src\main\resources\db\mysql\00-create-databases.sql"
cmd /c "mysql --default-character-set=utf8mb4 -u root -p heartbeat < heartbeat-start\src\main\resources\db\mysql\heartbeat-enterprise-all.sql"
```

生产或外部数据库使用 `prod` profile：

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:MYSQL_HOST="127.0.0.1"
$env:MYSQL_PORT="3306"
$env:MYSQL_DB="heartbeat"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="root"
mvn -pl heartbeat-start -am spring-boot:run
```

生产环境不会自动建表，建议执行 `db/mysql/heartbeat-enterprise-all.sql` 或交给 Flyway 管理。`db/heartbeat_quartz.sql` 仅包含 Quartz JDBC JobStore。

模块脚本修改后执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File heartbeat-start\src\main\resources\db\build-sql-bundles.ps1
```

应用启动时会幂等补齐默认菜单和基础种子，但不会代替正式 DDL 安装。

默认管理员：

- 用户名：`admin`
- 密码：`admin123`

该密码只用于首次初始化，正式环境登录后必须立即修改。支付、OAuth、数据库及消息中间件真实密钥不得写入 SQL 文件。

## 第二阶段边界

以下能力不放在第一阶段深做，只保留表和菜单入口：

- 服务监控、缓存监控、连接池监控。
- 定时任务真实调度执行器和任务日志聚合。
- 代码生成、在线构建器、接口文档聚合。
- 完整 OAuth2 Authorization Server / OIDC 单点登录服务化。
- 精细化数据权限 SQL 自动增强。

## 后端扩展模块收尾

本轮已补齐后端生产代码、初始化脚本与安全收尾，前端页面和新增后端测试类仍按约定留到后续统一处理。

### 扩展业务 API

- 工作流：`/api/v1/workflow/**`，支持流程定义创建/部署、发起实例、待办查询、审批通过/驳回。
- 支付：`/api/v1/pay/**`，支持支付渠道、下单、订单查询、模拟回调 HMAC-SHA256 验签与通知日志。
- 公众号：`/api/v1/mp/**`，支持账号、菜单、本地同步结果、素材和自动回复。
- 报表：`/api/v1/report/**`，支持数据集、模板、只读 SELECT 查询和 CSV 导出。
- 移动端：`/api/v1/mobile/**`，支持移动应用、页面配置和 API 路由配置。

### 初始化与权限

`heartbeat-start/src/main/resources/db/mysql/heartbeat-enterprise-all.sql` 与 H2 启动初始化均已包含：

- `wf_*`、`pay_*`、`mp_*`、`report_*`、`mobile_*` 业务表。
- `structure_publish_audit` 结构版本发布审计表。
- 业务平台菜单和按钮权限种子，超级管理员默认绑定所有菜单。

### 安全与审计

- 社交登录渠道 `appSecret` 写入时加密存储，管理端返回脱敏值，运行时解密供登录 Handler 使用。
- 新增 `@OperLog` 操作日志注解与 AOP，写入现有 `sys_oper_log`。
- 结构版本发布会记录发布人、版本号、结果和摘要到 `structure_publish_audit`。

### 代码生成

- React 页面模板已外置到 `heartbeat-start/src/main/resources/codegen-templates/react-page.jsx.tpl`。
- 预览和下载均包含 React 模板产物。
- ZIP 内生成路径按 DDD 多模块目录重映射到 `heartbeat-domain`、`heartbeat-application`、`heartbeat-infrastructure`、`heartbeat-interfaces`。
- 导入生成表后会同步创建业务菜单和按钮权限。

### 验收

已运行现有后端测试：

```powershell
mvn -pl heartbeat-start -am test
```

结果：通过。
