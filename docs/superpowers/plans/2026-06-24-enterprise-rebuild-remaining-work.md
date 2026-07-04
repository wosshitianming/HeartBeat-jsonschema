# HeartBeat 企业系统剩余改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已完成平台 IAM、认证基础表、代码生成与调度改造的基础上，彻底移除剩余雪花主键、字符串主键、通用 Map 仓储、动态 SQL 大仓储和旧 SQL 模板，使所有正式业务域达到可上线的企业级标准。

**Architecture:** MySQL 8 与 Flyway 是唯一正式数据库契约，所有应用业务表使用 `BIGINT UNSIGNED AUTO_INCREMENT` 主键。按认证、平台管理、结构智能、自动化审批、支付、公众号、报表和移动端拆分强类型领域模型；MyBatis-Flex Entity/Mapper/Repository 只服务单一领域，跨领域通过应用服务和 Outbox/Inbox 事件协作。

**Tech Stack:** Java 8、Spring Boot 2.7.18、MyBatis-Flex 1.11.7、MySQL 8.0、Flyway、Spring Security、JWT、Redis、RabbitMQ/Kafka、Quartz、JUnit 5、Testcontainers。

---

## 一、当前基线

### 已完成

- [x] 平台 IAM、组织、用户、角色、权限、菜单使用独立企业表。
- [x] 登录签发的 JWT 已包含用户、租户和会话标识。
- [x] `sys_gen_table`、`sys_gen_column`、`sys_job`、`sys_job_log` 已使用独立表和自增主键。
- [x] 代码生成和 Quartz 调度已切换到强类型仓储。
- [x] `sys_oper_log`、`sys_login_log` 已使用独立实体和 Mapper。
- [x] `PlatformResource`、`PlatformResourceMapper`、`PlatformFlexResourceSupport` 已删除。
- [x] 当前完整 Maven 测试通过；Docker 不可用时 MySQL Testcontainers 用例会跳过。

### 尚未完成

- [x] `auth_session` 尚未参与每次访问、刷新和退出时的有效性判断。
- [x] `AdminPlatformRepository`、`AdminPlatformService`、`AdminPlatformFlexRepository` 仍保留字符串资源名和 `Map<String, Object>` 通用 CRUD。
- [x] `structure_*`、`hb_flow_*`、`wf_*`、`pay_*`、`mp_*`、`report_*`、`mobile_*` 仍存在字符串主键。
- [x] `BusinessFlexRepository` 同时承担五个业务域，并通过可变 SQL 和 Map 访问数据库。
- [x] `StructurePublishAuditFlexRepository` 与 `BusinessFlexRepository` 仍使用 `SnowflakeIdGenerator`。
- [x] 旧 SQL 文件仍包含 `sys_resource_base` 和 `CREATE TABLE ... LIKE`。
- [x] 尚未生成唯一、无历史模板的正式聚合 SQL。
- [ ] 生产 MySQL 8 空库迁移、升级迁移和真实启动尚未完成最终验收。

## 二、改造顺序与发布门禁

| 阶段 | 范围 | 完成条件 |
| --- | --- | --- |
| A | 认证会话闭环 | 被撤销或过期会话无法访问、刷新；退出登录服务端立即生效 |
| B | 平台后台强类型拆分 | 平台 Repository/Service 不再暴露资源字符串与 Map CRUD |
| C | 结构智能与自动化主键改造 | 结构、流程和运行记录全部使用自增 Long 主键 |
| D | 人工审批与可靠事件 | 自动化等待审批后可通过 Outbox/Inbox 幂等恢复 |
| E | 支付、公众号、报表、移动端领域化 | 删除 `BusinessFlexRepository` 和 `BusinessSqlMapper` |
| F | SQL 单一事实源 | 只保留 Flyway 与一份由 Flyway 聚合的安装 SQL |
| G | 生产硬化 | MySQL 8、全量测试、打包、五环境配置和启动全部通过 |

每阶段必须满足：

- Maven 编译和当前全量测试继续通过。
- 新增写操作必须包含可信 `tenant_id`。
- 更新、删除必须同时约束 `tenant_id` 与主键。
- Repository 不得新增 `Map<String, Object>` 返回值。
- API 中 Long 主键统一序列化为字符串。
- 日志和返回体不得包含密码、Token、支付密钥和第三方平台密钥。

## Task 1：建立剩余改造的自动化守卫

**Files:**

- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseRebuildRemainingContractTest.java`
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/MybatisFlexOnlyPersistenceTest.java`
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseDatabaseContractTest.java`

- [x] **Step 1：编写当前应失败的残留扫描测试**

测试必须扫描生产源码和正式 SQL，并建立最终目标：

```java
assertSourceAbsent("SnowflakeIdGenerator");
assertSourceAbsent("BusinessFlexRepository");
assertSourceAbsent("BusinessSqlMapper");
assertSourceAbsent("listResource(String resource)");
assertSourceAbsent("createResource(String resource");
assertSourceAbsent("updateResource(String resource");
assertSourceAbsent("deleteResource(String resource");
assertSqlAbsent("sys_resource_base");
assertSqlAbsent("CREATE TABLE LIKE");
assertSqlAbsent("CREATE TABLE `");
```

最后一条只对旧兼容 SQL 目录执行，Flyway 目录不执行该断言。

- [x] **Step 2：增加字符串主键表扫描**

从 `schema.sql` 与 Flyway SQL 中提取目标业务表，断言其 `id` 最终为自增 Long：

```java
private static final List<String> AUTO_ID_TABLES = Arrays.asList(
        "structure_definition", "structure_version", "structure_publish_audit",
        "hb_node_component", "hb_flow_definition", "hb_flow_version",
        "hb_connection_credential", "hb_flow_run", "hb_flow_run_event",
        "wf_process_definition", "wf_process_instance", "wf_task", "wf_task_action",
        "pay_channel", "pay_order", "pay_notify_log",
        "mp_account", "mp_menu", "mp_material", "mp_auto_reply",
        "report_dataset", "report_template", "report_query_log",
        "mobile_app", "mobile_page", "mobile_api_route"
);
```

- [x] **Step 3：运行测试并记录预期失败**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnterpriseRebuildRemainingContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，失败项应准确列出剩余雪花 ID、Map 仓储、旧 SQL 和字符串主键。

## Task 2：完成认证会话生命周期

**Files:**

- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/auth/AuthSession.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/auth/AuthSessionRepository.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/auth/AuthenticationSessionService.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/auth/AuthSessionFlexRepository.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/security/JwtAuthenticationFilter.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/security/JwtTokenService.java`
- Modify: `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/auth/AuthController.java`
- Modify: `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/AdminPlatformService.java`
- Modify: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/AdminPlatformRepository.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/AdminPlatformFlexRepository.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/security/AuthenticationSessionTest.java`

- [x] **Step 1：先写会话状态测试**

覆盖以下行为：

```text
ACTIVE 且未过期的 session 可以访问
REVOKED session 无法访问
access token 已过期时无法访问
refresh token 哈希不匹配时无法刷新
refresh token 过期时无法刷新
刷新成功后旧 refresh token 立即失效
logout 将 session 更新为 REVOKED
logout 后旧 access token 无法继续访问
```

- [x] **Step 2：定义强类型会话仓储**

```java
public interface AuthSessionRepository {
    Optional<AuthSession> findActive(long tenantId, String sessionId);
    AuthSession create(AuthSession session);
    void rotateRefreshToken(long tenantId, String sessionId, String refreshTokenHash,
                            LocalDateTime refreshExpireAt);
    void touch(long tenantId, String sessionId, LocalDateTime lastAccessAt);
    void revoke(long tenantId, String sessionId, LocalDateTime revokedAt);
}
```

- [x] **Step 3：把刷新逻辑移出 `JwtTokenService`**

`JwtTokenService` 只负责签名、验签和解析 Claims。`AuthenticationSessionService.refresh` 执行：

```text
解析 refresh token
校验 token_type=refresh
读取 tid、uid、sid
查询 ACTIVE session
比较 SHA-256 refresh_token_hash
检查 refresh_expire_at
签发新 token 对
以新 refresh token 哈希覆盖旧哈希
返回新 token 对
```

- [x] **Step 4：过滤器验证服务端会话**

在创建 Spring Security Authentication 之前调用：

```java
AuthSession session = authenticationSessionService.requireActive(
        token.tenantId(), token.userId(), token.sessionId());
```

无有效会话时返回未认证状态，不加载权限。

- [x] **Step 5：实现服务端退出登录**

`POST /api/v1/auth/logout` 从当前请求读取 `heartbeatTenantId` 与 `heartbeatSessionId`，调用：

```java
authenticationSessionService.logout(tenantId, sessionId);
```

- [x] **Step 6：清理旧的默认会话方法**

从 `AdminPlatformRepository` 删除：

```java
default void createAuthSession(String userId, String sessionId, String refreshToken)
```

登录应用服务直接依赖 `AuthSessionRepository`。

- [x] **Step 7：运行认证测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=AuthenticationSessionTest,SecurityAuthorizationApiTest,TenantIsolationSecurityTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

## Task 3：拆分后台平台强类型 Repository 与 Service

**Files:**

- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/iam/UserRepository.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/iam/RoleRepository.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/iam/MenuRepository.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/organization/DepartmentRepository.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/config/ConfigurationRepository.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/config/UserPreferenceRepository.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/auth/SocialIdentityRepository.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/iam/UserApplicationService.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/iam/RoleApplicationService.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/iam/MenuApplicationService.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/organization/DepartmentApplicationService.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/config/ConfigurationApplicationService.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/iam/UserFlexRepository.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/iam/RoleFlexRepository.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/iam/MenuFlexRepository.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/organization/DepartmentFlexRepository.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/config/ConfigurationFlexRepository.java`
- Modify: `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/admin/AdminResourceController.java`
- Delete after callers migrate: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/AdminPlatformRepository.java`
- Delete after callers migrate: `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/AdminPlatformService.java`
- Delete after callers migrate: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/AdminPlatformFlexRepository.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/admin/TypedAdminApplicationServiceTest.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/admin/AdminPlatformApiTest.java`

- [x] **Step 1：为每个资源编写强类型服务测试**

测试用户、部门、角色、菜单、配置、字典、公告、偏好和社会化绑定的：

```text
创建、查询、更新、逻辑删除
租户隔离
唯一键冲突
乐观锁冲突
权限校验
API Long ID 字符串序列化
```

- [x] **Step 2：定义强类型模型和命令**

禁止领域层使用 Map。示例用户写入契约：

```java
public interface UserRepository {
    Optional<UserAccount> findById(long tenantId, long userId);
    Optional<UserAccount> findByUsername(long tenantId, String username);
    List<UserSummary> findAll(long tenantId, UserQuery query);
    UserAccount create(UserAccount account);
    UserAccount update(UserAccount account, int expectedVersion);
    void delete(long tenantId, long userId, long operatorId);
}
```

- [x] **Step 3：兼容控制器只做显式路由**

`AdminResourceController` 可以暂时保留旧 URL，但必须显式分派：

```java
switch (resource) {
    case "users":
        return Result.success(userApplicationService.list(request));
    case "depts":
        return Result.success(departmentApplicationService.list());
    case "roles":
        return Result.success(roleApplicationService.list());
    default:
        throw new IllegalArgumentException("Unsupported admin resource: " + resource);
}
```

兼容控制器不得访问 Mapper、不得拼接表名、不得调用通用仓储。

- [x] **Step 4：删除四个通用 CRUD 契约**

彻底删除：

```java
listResource(String resource)
createResource(String resource, Map<String, Object> command)
updateResource(String resource, String id, Map<String, Object> command)
deleteResource(String resource, String id)
```

- [x] **Step 5：删除三个平台大类**

所有调用者迁移后删除 `AdminPlatformRepository`、`AdminPlatformService` 和
`AdminPlatformFlexRepository`。

- [x] **Step 6：运行平台测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=TypedAdminApplicationServiceTest,AdminPlatformApiTest,SecurityAuthorizationApiTest,TypedAuthRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

## Task 4：把结构智能表改为企业自增主键

**Files:**

- Create: `heartbeat-start/src/main/resources/db/migration/mysql/V5__structure_intelligence.sql`
- Modify: `heartbeat-start/src/main/resources/schema.sql`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/StructureDefinitionEntity.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/StructureVersionEntity.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/StructurePublishAudit.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/structure/repository/StructurePublishAuditFlexRepository.java`
- Modify: structure domain IDs, repositories and application services under:
  `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/structure/`,
  `heartbeat-application/src/main/java/top/kx/heartbeat/application/structure/`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/structure/StructurePersistenceContractTest.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/structure/StructureDefinitionApiTest.java`

- [x] **Step 1：建立结构表 DDL**

`V5__structure_intelligence.sql` 创建：

```text
structure_definition
structure_draft
structure_version
structure_artifact
structure_publish_audit
```

全部使用：

```sql
`id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
```

租户私有表使用 `tenant_id BIGINT UNSIGNED NOT NULL`，发布版本以
`UNIQUE (tenant_id, definition_id, version_no)` 保证不可重复。

- [x] **Step 2：将草稿与正式版本分表**

`structure_definition` 只保存定义元数据和当前版本引用；草稿 JSON 放入
`structure_draft`；不可变版本内容放入 `structure_version`；产物放入
`structure_artifact`。

- [x] **Step 3：实体 ID 全部改为 Long**

```java
@Id(keyType = KeyType.Auto)
private Long id;
```

删除 `StructurePublishAuditFlexRepository` 对 `SnowflakeIdGenerator` 的依赖，
插入时不手动设置 ID。

- [x] **Step 4：API 边界兼容字符串 ID**

Controller PathVariable 继续接收字符串时，在 DTO/Assembler 层执行受控转换；
领域和持久化内部只使用 `long/Long`。

- [x] **Step 5：运行结构测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=StructurePersistenceContractTest,StructureDefinitionApiTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

## Task 5：改造自动化编排与人工审批，并建立可靠事件

**Files:**

- Create: `heartbeat-start/src/main/resources/db/migration/mysql/V6__automation_workflow_events.sql`
- Modify: `heartbeat-start/src/main/resources/schema.sql`
- Modify: existing flow entities under
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/`
- Create: workflow entities under
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/workflow/`
- Create: workflow mappers under
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper/workflow/`
- Create: event entities/mappers under
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/event/`
  and `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper/event/`
- Replace: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/workflow/WorkflowRepository.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/workflow/WorkflowApplicationService.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/workflow/WorkflowApprovalIntegrationTest.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/workflow/OutboxInboxIdempotencyTest.java`

- [x] **Step 1：创建企业化自动化和审批表**

迁移创建：

```text
flow_component
flow_definition
flow_version
flow_trigger
flow_credential
flow_run
flow_run_node
flow_wait_state
wf_definition
wf_definition_version
wf_instance
wf_task
wf_task_candidate
wf_task_action
sys_outbox_event
sys_inbox_event
```

所有表使用自增 Long 主键；运行记录、任务动作和事件只追加。

- [x] **Step 2：替换 Map 版审批仓储**

新的仓储必须返回：

```text
WorkflowDefinition
WorkflowInstance
WorkflowTask
WorkflowTaskAction
```

所有状态变化通过受控枚举和应用服务完成，不允许任意字符串更新状态。

- [x] **Step 3：实现自动化等待审批**

执行链路：

```text
flow_run 到达 APPROVAL 节点
创建 wf_instance 和首个 wf_task
创建 flow_wait_state，状态 WAITING
同事务写入 sys_outbox_event
人工审批完成后写入新的 Outbox 事件
消费者先写 sys_inbox_event
未消费过才恢复 flow_run
flow_wait_state 更新为 RESUMED
```

- [x] **Step 4：实现幂等键**

必须建立唯一键：

```text
wf_instance(tenant_id, business_type, business_key, definition_version_id)
flow_wait_state(tenant_id, correlation_key)
sys_outbox_event(event_id)
sys_inbox_event(consumer_code, event_id)
```

- [x] **Step 5：运行联动测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=WorkflowApprovalIntegrationTest,OutboxInboxIdempotencyTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS，重复审批事件和重复 MQ 消息不得重复推进流程。

## Task 6：拆分支付领域并实现状态机

**Files:**

- Create: `heartbeat-start/src/main/resources/db/migration/mysql/V7__payment.sql`
- Replace: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/pay/PayRepository.java`
- Create: payment models under `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/pay/model/`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/pay/PaymentApplicationService.java`
- Create: payment entities/mappers/repositories under
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/pay/`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/pay/PaymentStateMachineTest.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/pay/PaymentNotifyIdempotencyTest.java`

- [x] **Step 1：创建支付专用表**

创建：

```text
pay_channel
pay_order
pay_transaction
pay_refund
pay_notify_log
```

金额统一使用 `DECIMAL(20,4)`，主键为自增 Long，订单号、交易号、退款号使用独立业务编号。

- [x] **Step 2：定义支付状态机**

允许转换：

```text
CREATED -> PAYING -> PAID
CREATED/PAYING -> CLOSED
PAID -> PART_REFUNDED -> REFUNDED
```

应用服务拒绝越级、倒退和重复状态变更。

- [x] **Step 3：保证回调幂等**

`pay_notify_log` 使用 `(tenant_id, provider, notify_id)` 唯一键；验签成功且首次处理时才改变订单状态。

- [x] **Step 4：运行支付测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=PaymentStateMachineTest,PaymentNotifyIdempotencyTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

## Task 7：拆分公众号、报表和移动端领域

**Files:**

- Create: `heartbeat-start/src/main/resources/db/migration/mysql/V8__content_report_mobile.sql`
- Replace: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/mp/MpRepository.java`
- Replace: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/report/ReportRepository.java`
- Replace: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/mobile/MobileRepository.java`
- Create: focused application services under:
  `heartbeat-application/src/main/java/top/kx/heartbeat/application/mp/`,
  `heartbeat-application/src/main/java/top/kx/heartbeat/application/report/`,
  `heartbeat-application/src/main/java/top/kx/heartbeat/application/mobile/`
- Create: focused persistence implementations under:
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/mp/`,
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/report/`,
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/mobile/`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/mp/MpCredentialSecurityTest.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/report/ReportQuerySecurityTest.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/mobile/MobilePublicationTest.java`

- [x] **Step 1：创建领域专用表**

创建：

```text
mp_account
mp_menu
mp_material
mp_auto_reply
mp_sync_log
report_datasource
report_dataset
report_template
report_query_log
report_export_task
mobile_app
mobile_app_version
mobile_page
mobile_api_route
```

所有主键和引用字段改为 Long；稳定业务字段必须独立成列。

- [x] **Step 2：公众号密钥加密和脱敏**

`app_secret`、`token`、`aes_key` 保存密文；列表和详情 API 只返回掩码，不提供解密后的密钥。

- [x] **Step 3：报表查询禁止直接执行前端 SQL**

删除以下契约：

```java
List<Map<String, Object>> query(String sql, Map<String, Object> params, int limit);
```

替换为：

```java
ReportQueryResult execute(long tenantId, long datasetId,
                          Map<String, Object> parameters, ReportQueryLimit limit);
```

执行前必须完成只读语句校验、单语句校验、表白名单、参数绑定、超时和最大行数限制。

- [x] **Step 4：移动端只读取已发布版本**

草稿保存在 `mobile_app` 与编辑表；生产读取必须绑定不可变的 `mobile_app_version`。

- [x] **Step 5：运行三个领域测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=MpCredentialSecurityTest,ReportQuerySecurityTest,MobilePublicationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

## Task 8：删除业务大仓储、动态 SQL 与雪花 ID

**Files:**

- Delete: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/business/BusinessFlexRepository.java`
- Delete: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper/BusinessSqlMapper.java`
- Delete: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/common/id/SnowflakeIdGenerator.java`
- Modify: `heartbeat-infrastructure/pom.xml`
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/MybatisFlexOnlyPersistenceTest.java`
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseRebuildRemainingContractTest.java`

- [x] **Step 1：确认所有调用者已经迁移**

Run:

```powershell
rg -n "BusinessFlexRepository|BusinessSqlMapper|SnowflakeIdGenerator" heartbeat-domain heartbeat-application heartbeat-infrastructure heartbeat-interfaces heartbeat-start
```

Expected: 只剩待删除类和负向测试断言。

- [x] **Step 2：删除三个遗留实现**

删除大仓储、动态 SQL Mapper 和雪花 ID 生成器。

- [x] **Step 3：检查 JDBC 边界**

保留 `FlexCodegenEngine` 的 `DatabaseMetaData` 读取，因为它属于数据库结构探测，不是业务 CRUD。

Run:

```powershell
rg -n "JdbcTemplate|NamedParameterJdbcTemplate|getConnection\\(" heartbeat-infrastructure/src/main/java
```

Expected: 只允许 `FlexCodegenEngine` 中的数据库元数据读取。

- [x] **Step 4：检查 JDBC 依赖是否仍有必要**

Run:

```powershell
mvn --% -pl heartbeat-infrastructure dependency:tree -Dincludes=org.springframework:spring-jdbc,org.springframework.boot:spring-boot-starter-jdbc,com.zaxxer:HikariCP
```

如果 MyBatis-Flex starter 已提供数据源自动配置，则删除显式
`spring-boot-starter-jdbc`；否则保留并在 POM 注释中说明仅用于 DataSource 基础设施。

- [x] **Step 5：运行残留守卫**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnterpriseRebuildRemainingContractTest,MybatisFlexOnlyPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

## Task 9：清理旧 SQL 并生成唯一聚合 SQL

**Files:**

- Delete:
  `heartbeat-start/src/main/resources/db/mysql/01-structure.sql`
  `heartbeat-start/src/main/resources/db/mysql/02-platform.sql`
  `heartbeat-start/src/main/resources/db/mysql/03-flow.sql`
  `heartbeat-start/src/main/resources/db/mysql/04-workflow.sql`
  `heartbeat-start/src/main/resources/db/mysql/05-business.sql`
  `heartbeat-start/src/main/resources/db/mysql/06-tool.sql`
  `heartbeat-start/src/main/resources/db/mysql/07-quartz.sql`
  `heartbeat-start/src/main/resources/db/mysql/90-seed.sql`
  `heartbeat-start/src/main/resources/db/mysql/heartbeat-all.sql`
  `heartbeat-start/src/main/resources/db/heartbeat_sys.sql`
  `heartbeat-start/src/main/resources/db/schema-mysql.sql`
- Keep: `heartbeat-start/src/main/resources/db/mysql/00-create-databases.sql`
- Keep: `heartbeat-start/src/main/resources/db/mysql/heartbeat_quartz.sql`
- Create: `heartbeat-start/src/main/resources/db/mysql/heartbeat-enterprise-all.sql`
- Create: `scripts/build-enterprise-sql.ps1`
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/DatabaseSqlOrganizationTest.java`
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseDatabaseContractTest.java`

- [x] **Step 1：确定 SQL 唯一事实源**

正式 DDL 和种子数据只允许来自：

```text
db/migration/mysql/V1__enterprise_platform_iam_auth.sql
db/migration/mysql/V2__platform_seed.sql
db/migration/mysql/V3__permission_seed.sql
db/migration/mysql/V4__enterprise_tooling.sql
db/migration/mysql/V5__structure_intelligence.sql
db/migration/mysql/V6__automation_workflow_events.sql
db/migration/mysql/V7__payment.sql
db/migration/mysql/V8__content_report_mobile.sql
```

- [x] **Step 2：编写确定性聚合脚本**

`scripts/build-enterprise-sql.ps1` 按版本号排序拼接 Flyway 文件，在文件头加入：

```sql
-- Generated from Flyway migrations. Do not edit manually.
SET NAMES utf8mb4;
SET time_zone = '+00:00';
```

输出固定为：

```text
heartbeat-start/src/main/resources/db/mysql/heartbeat-enterprise-all.sql
```

- [x] **Step 3：聚合文件不得包含旧模型**

测试断言：

```text
不包含 sys_resource_base
不包含 CREATE TABLE ... LIKE
不包含 VARCHAR(36) NOT NULL PRIMARY KEY
包含 V1 到 V8 的版本分隔注释
聚合内容顺序与 Flyway 版本顺序一致
```

- [x] **Step 4：删除旧 SQL**

旧 SQL 删除后，README 和测试不得再引用旧文件。

- [x] **Step 5：运行 SQL 契约测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=DatabaseSqlOrganizationTest,EnterpriseDatabaseContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

## Task 10：完成配置和生产安全收口

**Files:**

- Modify: `heartbeat-start/src/main/resources/application.yml`
- Modify: `heartbeat-start/src/main/resources/application-local.yml`
- Modify: `heartbeat-start/src/main/resources/application-dev.yml`
- Modify: `heartbeat-start/src/main/resources/application-test.yml`
- Modify: `heartbeat-start/src/main/resources/application-pre.yml`
- Modify: `heartbeat-start/src/main/resources/application-gray.yml`
- Modify: `heartbeat-start/src/main/resources/application-prod.yml`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/security/SecurityConfig.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/tenant/HeartbeatTenantFactory.java`
- Modify: `heartbeat-application/src/main/java/top/kx/heartbeat/application/auth/AbstractJsonSocialLoginHandler.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnvironmentConfigurationTest.java`

- [x] **Step 1：统一端口与环境配置**

全部环境使用：

```yaml
server:
  port: 7001
```

生产、预发、灰度、测试和开发环境必须显式设置：

```yaml
heartbeat:
  security:
    dev-auto-login: false
    dev-header-enabled: false
```

仅 `local` 环境允许按需开启开发自动登录。

- [x] **Step 2：配置中间件通用参数**

每个非本地环境必须提供环境变量占位：

```text
MySQL URL、用户名、密码、连接池
Redis 地址、密码、数据库编号、超时
RabbitMQ 或 Kafka 地址、认证、重试
Flyway 开关与迁移目录
JWT 密钥、访问令牌和刷新令牌有效期
Quartz 集群配置
日志级别与脱敏
Actuator 健康检查
```

不得在配置文件提交真实密码。

- [x] **Step 3：移除 Spring 默认用户密码提示**

系统使用自定义 JWT 认证，不应依赖默认内存用户。调整安全配置或排除对应自动配置，使启动日志不再生成默认密码。

- [x] **Step 4：消除编译警告**

替换 `HeartbeatTenantFactory` 中已废弃 API，并修复
`AbstractJsonSocialLoginHandler` 的未经检查类型转换。

- [x] **Step 5：运行配置测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnvironmentConfigurationTest,TransactionManagerConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

## Task 11：在真实 MySQL 8 上完成最终验收

**Files:**

- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseMySqlMigrationTest.java`
- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseMySqlUpgradeTest.java`
- Modify: `README.md`
- Modify: `docs/saas-admin-mvp.md`

- [x] **Step 1：扩展 MySQL 空库迁移测试**

`EnterpriseMySqlMigrationTest` 必须验证 V1-V8 所有业务表：

```text
主键为 bigint unsigned auto_increment
tenant_id 为 bigint unsigned
业务唯一索引包含 tenant_id
不存在 sys_resource_base
不存在字符串主键应用表
```

- [x] **Step 2：增加升级路径测试**

`EnterpriseMySqlUpgradeTest`：

```text
先只执行 V1-V4
插入平台、用户、工具测试数据
再执行 V5-V8
验证原数据保留
验证新表可读写
验证 Flyway schema history 到达 V8
```

- [ ] **Step 3：Docker 环境执行 MySQL 测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnterpriseMySqlMigrationTest,EnterpriseMySqlUpgradeTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 两个测试均实际执行并 PASS，不接受 skipped 作为生产验收结果。

当前记录：已执行该命令，但当前机器未安装或无法访问 Docker（`docker` 命令不可用，Testcontainers 找不到 `//./pipe/docker_engine`），两个 MySQL 8 Testcontainers 测试被 skipped；因此本步骤未达到生产验收要求。

- [x] **Step 4：运行完整测试**

Run:

```powershell
mvn --% -pl heartbeat-start -am test
```

Expected: BUILD SUCCESS，零失败、零错误；只有与生产验收无关且有明确原因的测试可以跳过。

- [x] **Step 5：打包**

Run:

```powershell
mvn --% -pl heartbeat-start -am -DskipTests package
```

Expected: `heartbeat-start/target/heartbeat.jar` 生成成功。

- [ ] **Step 6：验证五个部署环境**

依次启动：

```powershell
java -jar heartbeat-start\target\heartbeat.jar --spring.profiles.active=dev
java -jar heartbeat-start\target\heartbeat.jar --spring.profiles.active=test
java -jar heartbeat-start\target\heartbeat.jar --spring.profiles.active=pre
java -jar heartbeat-start\target\heartbeat.jar --spring.profiles.active=gray
java -jar heartbeat-start\target\heartbeat.jar --spring.profiles.active=prod
```

Expected: 所有环境监听 7001，生产类环境不允许自动登录和用户头模拟认证。

当前记录：已尝试启动 `dev` profile，应用在 Flyway 初始化阶段因无法连接 `127.0.0.1:3306` MySQL 报 `Communications link failure / Connection refused` 后退出。五环境启动仍需要可用的 MySQL、Redis、RabbitMQ/Kafka 等外部依赖后再验收。

## 三、最终验收清单

- [x] 生产源码中不存在 `SnowflakeIdGenerator`。
- [x] 生产源码中不存在 `BusinessFlexRepository` 和 `BusinessSqlMapper`。
- [x] 不存在字符串资源名驱动的后台 CRUD。
- [x] 领域 Repository 不返回 `Map<String, Object>`。
- [x] 所有应用业务表使用自增 Long 主键。
- [x] Quartz 官方表保持 Quartz 官方主键结构。
- [x] 代码生成器只在元数据探测场景使用 JDBC `DatabaseMetaData`。
- [x] 会话撤销、退出登录、刷新令牌轮换全部服务端生效。
- [x] 自动化、人工审批、Outbox、Inbox 能形成幂等闭环。
- [x] 支付回调和状态迁移具备幂等与状态机保护。
- [x] 报表 SQL 不能由前端任意提交执行。
- [x] 密码、Token、支付密钥、公众号密钥不明文存储或输出。
- [x] 旧 SQL 模板全部删除。
- [ ] `heartbeat-enterprise-all.sql` 可在 MySQL 8 空库一次执行成功。
- [ ] Flyway V1-V8 可在 MySQL 8 空库和升级路径执行成功。
- [ ] 完整 Maven 测试、打包和五环境启动均通过。

## 四、执行建议

推荐按 Task 1 → Task 11 严格顺序实施。Task 2 和 Task 3 是后台安全与可维护性收口；
Task 4 至 Task 7 是业务域正式重建；Task 8 和 Task 9 只能在所有调用者迁移完成后执行；
Task 10 和 Task 11 是上线前门禁，不应提前标记完成。

当前工作区若仍未初始化为有效 Git 仓库，则每个 Task 完成后至少保留测试输出记录；
若后续启用 Git，每个 Task 单独提交，不把多个领域混入同一提交。
