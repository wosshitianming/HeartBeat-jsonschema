# Flow 事件驱动运行时设计文档：Flowable 集成方案

## 1. 结论

HeartBeat 的 Open Flow Studio 不再继续自研完整生产态流程引擎。生产态运行时采用 Flowable 嵌入式流程引擎，HeartBeat 保留自己的 Flow DSL、组件注册、凭据管理、权限审计、租户模型、可视化画布和运行记录投影。

核心分工：

| 层 | 职责 | 不负责 |
| --- | --- | --- |
| Open Flow Studio | 画布、节点组件、节点配置、连线、版本、发布、运行历史展示 | 自己维护生产态 token 流转、等待恢复、任务调度 |
| Flow DSL | HeartBeat 自己的可视化流程契约 | 直接作为生产执行格式 |
| Flow DSL Compiler | 将 Flow DSL 编译为 BPMN XML | 运行节点业务逻辑 |
| Flowable | 流程实例、节点推进、网关、人工任务、等待、定时器、异步 Job、历史 | HeartBeat 业务权限、凭据、节点组件市场 |
| HeartBeat Node Delegate | Flowable 进入 serviceTask 时调用 HeartBeat 节点执行器 | 保存 Flowable 内部运行状态 |
| Outbox/Inbox | 跨领域事件可靠投递、幂等消费、外部事件恢复流程 | 替代 Flowable 自身流程实例状态 |

设计目标不是把 HeartBeat 变成纯 BPM 平台，而是让 n8n 类的节点编排获得稳定的生产态执行能力。

## 2. 当前系统现状

### 2.1 已有能力

当前仓库已经存在这些基础：

- Flow DSL 模型：`heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/model/FlowDefinition.java`
- 节点模型：`FlowNode`
- 连线模型：`FlowEdge`
- 节点组件清单：`NodeComponentManifest`
- 组件注册服务：`heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/NodeComponentRegistryService.java`
- 同步调试执行器：`heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowExecutor.java`
- 节点执行接口：`NodeExecutor`
- 调试结果和节点事件：`FlowDebugResult`、`FlowRunEvent`
- Flow API：`heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/flow/FlowController.java`
- 人工审批基础模块：`heartbeat-application/src/main/java/top/kx/heartbeat/application/workflow/WorkflowService.java`
- BPMN XML 解析入口：`heartbeat-application/src/main/java/top/kx/heartbeat/application/workflow/pipeline/BpmnWorkflowDefinitionCommandHandler.java`
- Outbox/Inbox/等待状态服务：`heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/event/ReliableWorkflowEventService.java`
- Kafka/RocketMQ/MQTT 依赖和开关已存在，但没有实际 Flow 生产态监听器。

### 2.2 主要缺口

当前 `FlowExecutor.debug()` 只能解决本地调试：

- 入口节点是“无输入端口节点”或第一个节点，不是生产态触发器。
- 执行队列是内存 `ArrayDeque`，进程退出后不可恢复。
- 节点执行完成后只靠 `nextPorts` 找下一条边，没有可持久化 token。
- 没有生产态 `run` 生命周期：`RUNNING`、`WAITING`、`FAILED`、`CANCELED`、`COMPLETED`。
- 没有异步重试、定时器、长等待、人工任务恢复。
- `builtin:mq.consume`、`builtin:trigger.webhook` 等目前只是占位执行器，不是真正监听外部事件。
- Outbox/Inbox 与 Flow 执行器尚未形成通用恢复链路。

### 2.3 不能继续只补当前执行器的原因

如果继续在 `FlowExecutor` 上补生产能力，需要逐步实现：

- 持久化执行 token
- Job 调度器
- 定时器
- 并发锁
- 人工任务
- 等待事件
- 网关表达式
- 历史记录
- 节点重试
- 流程取消
- 流程迁移
- 运行恢复

这会把 Open Flow Studio 变成一个自研 BPM/工作流内核，风险高、周期长，并且与现有计划中“未来参考 Temporal / Camunda / Zeebe”的方向重叠。

## 3. 选型

### 3.1 选择 Flowable

本方案选择 Flowable 作为生产态运行时，原因：

- 当前项目是 Spring Boot 2.7.18、Java 8，多模块 Maven，Flowable 6.x 与该技术栈匹配度高。
- 当前代码已有 BPMN XML 解析入口，并兼容 `flowable:assignee`。
- Flowable 支持 BPMN 2.0 的开始、结束、顺序流、网关、服务任务、用户任务、接收任务、定时器、异步 Job、历史。
- Flowable 可以嵌入式运行，不要求引入额外独立平台。
- Flowable 有 `tenantId` 概念，可与 HeartBeat 租户模型对接。
- Flowable UserTask 可承接当前 `workflow` 人工审批模块的长期演进。

版本建议：

- Spring Boot 2.7.18 + Java 8 下首选 Flowable `6.8.0` 作为集成基线。
- 如果实施阶段发现依赖冲突，只允许在 Flowable `6.8.x` 补丁线内调整，并在父 POM 固定具体版本号。
- 不得使用动态版本号，不得使用 `LATEST`、`RELEASE` 或版本范围。
- 不在生产环境开启 Flowable 自动建表，Flowable 表结构进入 Flyway 版本脚本。

### 3.2 暂不选择 Temporal

Temporal 很适合代码式长事务和可靠任务编排，但不适合作为当前第一阶段方案：

- 需要独立 Temporal Server。
- Java workflow 代码模型与当前 Flow DSL / BPMN / 可视化画布差异较大。
- 人工审批、BPMN 图、业务用户可视化理解成本更高。

Temporal 可以作为后续“开发者工作流”或“代码式可靠任务”的参考，不作为 Open Flow Studio 第一阶段运行时。

### 3.3 暂不选择 Camunda / Zeebe

Camunda 7 可嵌入，但后续生态重心已转向 Camunda 8 / Zeebe。Zeebe 更偏分布式外部服务和 Job Worker 模式，首期引入成本高。

HeartBeat 当前优先需要：

- 嵌入式
- 低运维
- Spring Boot 2.7 / Java 8 兼容
- BPMN + UserTask + ServiceTask

Flowable 更适合当前落点。

## 4. 总体架构

```text
前端 Open Flow Studio
  -> 保存 Flow DSL
  -> 编译校验
  -> 发布版本

FlowApplicationService
  -> FlowDslValidator
  -> FlowBpmnCompiler
  -> FlowableDeploymentService
  -> hb_flow_version 写入 BPMN/部署信息

触发入口
  -> Manual API / Webhook / Cron / MQ / Domain Event
  -> FlowTriggerService
  -> 幂等检查
  -> RuntimeService.startProcessInstanceById(...)

Flowable Runtime
  -> startEvent
  -> serviceTask
  -> exclusiveGateway
  -> userTask
  -> receiveTask/message/timer
  -> endEvent

FlowableNodeDelegate
  -> NodeComponentRepository
  -> NodeExecutorRegistry
  -> ConnectionCredentialService
  -> 具体 NodeExecutor 或创建 hb_flow_io_command
  -> 写受控变量和 payloadRef

FlowableProjectionEventListener
  -> 监听 Flowable 全局事件
  -> 写 hb_flow_run / hb_flow_run_event 投影

外部事件恢复
  -> MQ/Webhook/审批完成事件
  -> Inbox 幂等
  -> hb_flow_wait_subscription 匹配或 EARLY_ARRIVED
  -> RuntimeService.messageEventReceived(...) 或 taskService.complete(...)
  -> Flowable 继续推进
```

关键原则：

- Flow DSL 是 HeartBeat 画布契约。
- BPMN XML 是生产执行契约。
- Flowable 的 `processInstanceId` 是生产流程实例主标识。
- HeartBeat 的 `hb_flow_run` 是面向业务和前端展示的运行记录投影。
- Flowable 历史表保留完整引擎历史，HeartBeat 运行事件表保留业务友好的节点输入/输出摘要。

## 5. 模块边界

### 5.1 domain 层

保留纯领域模型，不依赖 Flowable：

```text
heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/model/
  FlowDefinition
  FlowNode
  FlowEdge
  FlowVersion
  FlowRun
  FlowRunEvent
  NodeComponentManifest
  ComponentPort
  ComponentRuntime
```

新增或调整领域枚举：

```text
FlowRuntimeEngine
  LOCAL_DEBUG
  FLOWABLE

FlowTriggerType
  MANUAL
  WEBHOOK
  CRON
  MQ
  DOMAIN_EVENT

FlowRunStatus
  CREATED
  RUNNING
  WAITING
  SUCCESS
  FAILED
  CANCELED
  TIMEOUT

FlowNodeRunStatus
  CREATED
  RUNNING
  WAITING
  SUCCESS
  FAILED
  SKIPPED
  RETRYING
```

domain 层不出现 `org.flowable.*`。

### 5.2 application 层

新增编译和运行编排服务：

```text
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/
  FlowBpmnCompiler
  FlowBpmnCompileResult
  FlowBpmnElementMapping
  FlowRuntimeFacade
  FlowTriggerCommand
  FlowStartCommand
  FlowResumeCommand
  NodeExecutionCommand
  NodeExecutionOutcome
  NodeExecutor
  NodeExecutorRegistry
```

职责：

- 校验 DSL。
- 编译 DSL 到 BPMN XML。
- 决定使用本地 debug 还是 Flowable。
- 组织节点执行命令。
- 不直接调用 Flowable API。

### 5.3 infrastructure 层

新增 Flowable 适配：

```text
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/
  FlowableDeploymentService
  FlowableRuntimeService
  FlowableHistoryQueryService
  FlowableNodeDelegate
  FlowableProjectionEventListener
  FlowableTaskBridge
  FlowableEventBridge
  FlowableTenantResolver
  FlowableVariableCodec
  FlowablePayloadStore
  FlowableExceptionTranslator
  FlowExternalIoCommandDispatcher
  FlowExternalIoReconcileJob
  FlowWaitRegistrationListener
  FlowConditionEvaluator
  FlowExpressionSandbox
  FlowProjectionPublisher
```

职责：

- 调用 Flowable `RepositoryService` 部署 BPMN。
- 调用 Flowable `RuntimeService` 启动/恢复实例。
- 调用 Flowable `TaskService` 查询/完成 UserTask。
- 调用 Flowable `HistoryService` 查询历史。
- 将 Flowable delegate 调用转换为 HeartBeat `NodeExecutor` 调用。
- 通过 Flowable 全局事件监听器集中投影 `hb_flow_run`、`hb_flow_run_event`。
- 将 Flowable 异常翻译为 HeartBeat 用户可读的节点错误。
- 对大 payload 执行瘦变量策略，避免 Flowable 变量表膨胀。
- 将长耗时 I/O 节点转为外部工作者命令，不让 Flowable Async Executor 承担阻塞式远程调用。
- 在等待节点进入时登记等待订阅，并消费提前到达的 Inbox 事件。
- 用受控表达式解释器执行条件判断，避免用户 DSL 直接泄漏为黑盒 UEL。
- 为未来 Flowable 引擎库和 HeartBeat 业务库拆分预留同步/异步投影切换接口。

### 5.4 interfaces 层

保留现有 `/api/v1/flows`，新增生产运行入口：

```text
POST /api/v1/flows/{id}/publish
POST /api/v1/flows/{id}/run
POST /api/v1/flows/{id}/debug
GET  /api/v1/flows/{id}/runs
GET  /api/v1/flows/runs/{runId}
GET  /api/v1/flows/runs/{runId}/events
GET  /api/v1/flows/runs/{runId}/replay
POST /api/v1/flows/runs/{runId}/cancel
POST /api/v1/flows/runs/{runId}/restart-on-active-version
POST /api/v1/flows/runs/{runId}/admin-move
POST /api/v1/flows/runs/{runId}/retry

POST /api/v1/flow-webhooks/{webhookKey}

GET  /api/v1/flow-tasks/todo
POST /api/v1/flow-tasks/{taskId}/complete
POST /api/v1/flow-tasks/{taskId}/reject
```

旧 `/api/v1/workflow/**` 暂时保留，通过 `FlowableTaskBridge` 逐步迁移。

## 6. Maven 和配置

### 6.1 父 POM

在根 `pom.xml` 增加：

```xml
<flowable.version>6.8.0</flowable.version>
```

在 `dependencyManagement` 增加：

```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter-process</artifactId>
    <version>${flowable.version}</version>
</dependency>
```

可选依赖：

```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter-actuator</artifactId>
    <version>${flowable.version}</version>
</dependency>
```

### 6.2 子模块

`heartbeat-infrastructure/pom.xml` 增加：

```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter-process</artifactId>
</dependency>
```

不在 domain/application 引入 Flowable。

### 6.3 应用配置

`heartbeat-start/src/main/resources/application.properties` 增加：

```properties
heartbeat.flow.runtime.engine=FLOWABLE
heartbeat.flow.runtime.debug-engine=LOCAL_DEBUG
heartbeat.flow.flowable.deploy-on-publish=true
heartbeat.flow.flowable.sync-history-projection=true

flowable.database-schema-update=false
flowable.async-executor-activate=true
flowable.history-level=full
flowable.check-process-definitions=false
```

说明：

- `flowable.database-schema-update=false`：生产禁止自动建表。
- `flowable.async-executor-activate=true`：启用异步 Job。
- `flowable.history-level=full`：保留完整历史，便于运行轨迹和审计。
- `flowable.check-process-definitions=false`：不扫描 classpath BPMN，所有流程由 HeartBeat 发布接口部署。

### 6.4 中间件开关

现有：

```properties
heartbeat.middleware.rocketmq.enabled=false
heartbeat.middleware.kafka.enabled=false
```

Flow 触发器必须服从这些开关：

- RocketMQ 未启用时，不创建 RocketMQ Flow trigger listener。
- Kafka 未启用时，不创建 Kafka Flow trigger listener。
- 对应流程发布可以成功，但触发器状态标记为 `DISABLED_BY_MIDDLEWARE`。

## 7. 数据库设计

### 7.1 Flowable 原生表

Flowable 会使用 `ACT_*` 表。生产环境必须通过 Flyway 创建，不允许运行时自动创建。

建议新增迁移：

```text
heartbeat-start/src/main/resources/db/migration/mysql/V7__flowable_engine_tables.sql
```

内容来源：

- Flowable 官方 MySQL 建表 SQL。
- 按当前项目 MySQL 8 规范审查字段类型、索引和字符集。
- 不修改 Flowable 表名和字段名，避免升级困难。

关键表族：

| 表族 | 用途 |
| --- | --- |
| `ACT_RE_*` | 流程定义、部署、模型资源 |
| `ACT_RU_*` | 运行中流程实例、任务、执行 token、变量、Job |
| `ACT_HI_*` | 历史流程实例、历史任务、历史活动、历史变量 |
| `ACT_GE_*` | 通用字节数组、属性 |
| `ACT_ID_*` | Flowable 身份表。本方案不使用其用户体系 |

不使用 Flowable 身份表做 HeartBeat 用户权限。用户、角色、菜单、租户仍使用 HeartBeat 自己的 IAM。

### 7.2 HeartBeat Flow 表调整

现有 `hb_flow_definition` 保留。需要调整或新增字段：

```sql
ALTER TABLE hb_flow_definition
  ADD COLUMN runtime_engine VARCHAR(32) NOT NULL DEFAULT 'FLOWABLE',
  ADD COLUMN active_process_definition_id VARCHAR(128) NULL,
  ADD COLUMN active_deployment_id VARCHAR(128) NULL;
```

`hb_flow_version` 增加：

```sql
ALTER TABLE hb_flow_version
  ADD COLUMN runtime_engine VARCHAR(32) NOT NULL DEFAULT 'FLOWABLE',
  ADD COLUMN bpmn_xml MEDIUMTEXT NULL,
  ADD COLUMN bpmn_sha256 CHAR(64) NULL,
  ADD COLUMN deployment_id VARCHAR(128) NULL,
  ADD COLUMN process_definition_id VARCHAR(128) NULL,
  ADD COLUMN process_definition_key VARCHAR(128) NULL,
  ADD COLUMN compile_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  ADD COLUMN compile_error TEXT NULL,
  ADD COLUMN deployed_at DATETIME(3) NULL;
```

发布后的版本不可修改：

- `flow_dsl` 不可修改。
- `bpmn_xml` 不可修改。
- `process_definition_id` 不可改写，只允许新增版本。

### 7.3 触发器表

新增 `hb_flow_trigger`：

```sql
CREATE TABLE hb_flow_trigger (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  flow_id BIGINT UNSIGNED NOT NULL,
  flow_version_id BIGINT UNSIGNED NULL,
  trigger_code VARCHAR(128) NOT NULL,
  trigger_type VARCHAR(32) NOT NULL,
  webhook_key VARCHAR(128) NULL,
  cron_expression VARCHAR(128) NULL,
  event_topic VARCHAR(128) NULL,
  mq_type VARCHAR(32) NULL,
  mq_topic VARCHAR(255) NULL,
  mq_tag VARCHAR(255) NULL,
  config_json JSON NULL,
  status VARCHAR(32) NOT NULL,
  last_triggered_at DATETIME(3) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL,
  updated_by BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_flow_trigger_code (tenant_id, trigger_code),
  UNIQUE KEY uk_flow_webhook_key (tenant_id, webhook_key),
  KEY idx_flow_trigger_flow (tenant_id, flow_id),
  KEY idx_flow_trigger_type_status (tenant_id, trigger_type, status)
);
```

触发器状态：

```text
DRAFT
ACTIVE
DISABLED
DISABLED_BY_MIDDLEWARE
ERROR
```

### 7.4 运行投影表

`hb_flow_run` 调整：

```sql
ALTER TABLE hb_flow_run
  ADD COLUMN run_no VARCHAR(64) NULL,
  ADD COLUMN engine VARCHAR(32) NOT NULL DEFAULT 'FLOWABLE',
  ADD COLUMN engine_instance_id VARCHAR(128) NULL,
  ADD COLUMN process_definition_id VARCHAR(128) NULL,
  ADD COLUMN flow_version_id BIGINT UNSIGNED NULL,
  ADD COLUMN trigger_id BIGINT UNSIGNED NULL,
  ADD COLUMN trigger_key VARCHAR(128) NULL,
  ADD COLUMN idempotency_key VARCHAR(128) NULL,
  ADD COLUMN idempotency_scope VARCHAR(32) NOT NULL DEFAULT 'START',
  ADD COLUMN business_key VARCHAR(128) NULL,
  ADD COLUMN correlation_key VARCHAR(128) NULL,
  ADD COLUMN parent_run_id BIGINT UNSIGNED NULL,
  ADD COLUMN root_run_id BIGINT UNSIGNED NULL,
  ADD COLUMN retry_from_run_id BIGINT UNSIGNED NULL,
  ADD COLUMN retry_no INT UNSIGNED NOT NULL DEFAULT 0,
  ADD COLUMN retry_reason VARCHAR(255) NULL,
  ADD COLUMN tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 1;
```

唯一约束：

```sql
CREATE UNIQUE INDEX uk_flow_run_idempotency
ON hb_flow_run (tenant_id, idempotency_scope, idempotency_key);
```

说明：

- `idempotency_scope = START` 表示原始启动幂等。
- `idempotency_scope = USER_RETRY` 表示普通业务用户复原重试幂等。
- `idempotency_scope = ADMIN_RESTART` 表示管理员取消重开幂等。
- 同一个旧 run 可以产生多个 retry run，但每次 retry 必须有新的 `idempotencyKey`。
- `parent_run_id` 指向直接来源 run，管理员重开和普通 retry 都要填写。
- `root_run_id` 指向第一次启动产生的 run；`retry_from_run_id` 指向本次普通用户复原重试的直接来源。

`hb_flow_run_event` 调整：

```sql
ALTER TABLE hb_flow_run_event
  ADD COLUMN event_seq BIGINT UNSIGNED NULL,
  ADD COLUMN engine_activity_id VARCHAR(128) NULL,
  ADD COLUMN execution_id VARCHAR(128) NULL,
  ADD COLUMN task_id VARCHAR(128) NULL,
  ADD COLUMN source_node_id VARCHAR(128) NULL,
  ADD COLUMN target_node_id VARCHAR(128) NULL,
  ADD COLUMN edge_id VARCHAR(128) NULL,
  ADD COLUMN token_id VARCHAR(128) NULL,
  ADD COLUMN attempt_no INT UNSIGNED NOT NULL DEFAULT 1,
  ADD COLUMN selected_ports JSON NULL,
  ADD COLUMN input_payload_ref BIGINT UNSIGNED NULL,
  ADD COLUMN output_payload_ref BIGINT UNSIGNED NULL,
  ADD COLUMN event_summary JSON NULL,
  ADD COLUMN error_code VARCHAR(64) NULL,
  ADD COLUMN tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 1;
```

推荐索引：

```sql
CREATE UNIQUE INDEX uk_flow_run_event_seq
ON hb_flow_run_event (tenant_id, run_id, event_seq);

CREATE INDEX idx_flow_run_event_node
ON hb_flow_run_event (tenant_id, run_id, source_node_id, event_type);

CREATE INDEX idx_flow_run_event_time
ON hb_flow_run_event (tenant_id, created_at);
```

运行回放要求：

- `event_seq` 在同一个 run 内单调递增，用于前端按时间回放。
- `source_node_id`、`target_node_id`、`edge_id` 用于高亮真实经过的画布连线。
- `token_id` 映射 Flowable execution/token，用于并行分支回放；前端默认隐藏该底层概念。
- `input_payload_ref`、`output_payload_ref` 指向脱敏后的 payload 摘要或详情。
- `event_summary` 保存前端无需再次聚合即可展示的节点状态、耗时、错误摘要。

事件类型：

```text
FLOW_STARTED
FLOW_COMPLETED
FLOW_FAILED
FLOW_CANCELED
FLOW_RETRIED
NODE_STARTED
NODE_COMPLETED
NODE_FAILED
NODE_WAITING
NODE_RETRYING
IO_COMMAND_CREATED
IO_COMMAND_RETRYING
IO_COMMAND_COMPLETED
IO_COMMAND_FAILED
IO_COMMAND_AMBIGUOUS
USER_TASK_CREATED
USER_TASK_COMPLETED
TIMER_WAITING
MESSAGE_WAITING
EXTERNAL_EVENT_RECEIVED
FLOW_WAIT_EVENT_EXPIRED
```

### 7.5 Flowable 与 HeartBeat ID 对照

新增 `hb_flow_engine_mapping`：

```sql
CREATE TABLE hb_flow_engine_mapping (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  flow_id BIGINT UNSIGNED NOT NULL,
  flow_version_id BIGINT UNSIGNED NOT NULL,
  flow_node_id VARCHAR(128) NOT NULL,
  bpmn_element_id VARCHAR(128) NOT NULL,
  component_type VARCHAR(128) NOT NULL,
  component_version VARCHAR(32) NOT NULL,
  executor_id VARCHAR(128) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_flow_node_mapping (tenant_id, flow_version_id, flow_node_id),
  UNIQUE KEY uk_bpmn_element_mapping (tenant_id, flow_version_id, bpmn_element_id)
);
```

用途：

- 前端运行轨迹从 BPMN activityId 映射回画布 nodeId。
- delegate 根据 BPMN elementId 找到 Flow node config。
- 历史查询时把 Flowable 历史活动映射成 HeartBeat 节点事件。

### 7.6 大 Payload 存储表

新增 `hb_flow_payload` 作为默认大 payload 存储。Redis/MongoDB 可以作为后续扩展，但首期不引入新存储依赖。

```sql
CREATE TABLE hb_flow_payload (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  run_id BIGINT UNSIGNED NOT NULL,
  node_id VARCHAR(128) NULL,
  payload_role VARCHAR(32) NOT NULL,
  content_type VARCHAR(64) NOT NULL,
  payload_sha256 CHAR(64) NOT NULL,
  payload_size BIGINT UNSIGNED NOT NULL,
  payload_json MEDIUMTEXT NULL,
  summary_json JSON NULL,
  expire_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_flow_payload_run (tenant_id, run_id),
  KEY idx_flow_payload_node (tenant_id, run_id, node_id),
  KEY idx_flow_payload_expire (expire_at)
);
```

字段说明：

- `payload_role`：`NODE_INPUT`、`NODE_OUTPUT`、`EXTERNAL_EVENT`、`HTTP_RESPONSE`、`QUERY_RESULT`。
- `payload_json`：只保存脱敏后的业务数据。
- `summary_json`：保存前端可快速展示的摘要，例如行数、字段名、截断标记。
- `expire_at`：支持按租户配置保留期限。

Flowable 变量只保存 `payloadRef.id`、`sha256` 和 `summary`。

### 7.7 外部 I/O 命令表

长耗时 I/O 节点不直接占用 Flowable Async Executor 线程。进入该节点时，Flowable delegate 只创建 I/O 命令，然后流程进入等待节点。

新增 `hb_flow_io_command`：

```sql
CREATE TABLE hb_flow_io_command (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  run_id BIGINT UNSIGNED NOT NULL,
  flow_version_id BIGINT UNSIGNED NOT NULL,
  node_id VARCHAR(128) NOT NULL,
  engine_instance_id VARCHAR(128) NOT NULL,
  execution_id VARCHAR(128) NULL,
  command_type VARCHAR(64) NOT NULL,
  worker_topic VARCHAR(128) NOT NULL,
  correlation_key VARCHAR(128) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  external_idempotency_required TINYINT(1) NOT NULL DEFAULT 1,
  external_idempotency_supported TINYINT(1) NOT NULL DEFAULT 1,
  external_idempotency_header VARCHAR(64) NOT NULL DEFAULT 'Idempotency-Key',
  external_call_policy VARCHAR(32) NOT NULL DEFAULT 'IDEMPOTENT_RETRY',
  external_request_id VARCHAR(128) NULL,
  external_status_query_key VARCHAR(128) NULL,
  input_payload_ref BIGINT UNSIGNED NULL,
  output_payload_ref BIGINT UNSIGNED NULL,
  status VARCHAR(32) NOT NULL,
  attempt_no INT UNSIGNED NOT NULL DEFAULT 0,
  max_attempts INT UNSIGNED NOT NULL DEFAULT 3,
  next_attempt_at DATETIME(3) NULL,
  locked_by VARCHAR(128) NULL,
  locked_until DATETIME(3) NULL,
  call_started_at DATETIME(3) NULL,
  ambiguous_at DATETIME(3) NULL,
  reconcile_attempt_no INT UNSIGNED NOT NULL DEFAULT 0,
  next_reconcile_at DATETIME(3) NULL,
  last_reconciled_at DATETIME(3) NULL,
  manual_required_at DATETIME(3) NULL,
  risk_level VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  timeout_at DATETIME(3) NULL,
  last_error_code VARCHAR(64) NULL,
  last_error_message VARCHAR(512) NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_flow_io_idempotency (tenant_id, idempotency_key),
  UNIQUE KEY uk_flow_io_correlation (tenant_id, correlation_key),
  KEY idx_flow_io_pending (tenant_id, worker_topic, status, next_attempt_at),
  KEY idx_flow_io_run (tenant_id, run_id, node_id)
);
```

状态：

```text
PENDING
LOCKED
CALL_PREPARED
CALL_STARTED
SUCCEEDED
FAILED_RETRYABLE
FAILED_FINAL
FAILED_AMBIGUOUS
RECONCILING
MANUAL_REQUIRED
TIMEOUT
CANCELED
```

外部工作者只消费 `hb_flow_io_command`，不直接推进 Flowable execution tree。工作者完成后写 Inbox 事件，由 `FlowResumeService` 统一唤醒等待节点。

外部幂等硬约束：

- `idempotency_key` 不只是 HeartBeat 内部幂等键，必须穿透到下游系统。
- HTTP 节点必须把该值写入 `external_idempotency_header` 对应 header，默认 `Idempotency-Key`；兼容旧系统时可以配置为 `X-Idempotency-Key`。
- MQ 节点必须把该值写入 message key、header 或业务 payload 中的幂等字段。
- 数据库写节点首期不开放；后续开放时必须要求目标表有唯一键、请求流水号或业务幂等表。
- 组件 manifest 必须声明 `supportsExternalIdempotency`。如果下游不支持外部幂等，也没有可靠状态查询接口，则 `external_call_policy` 必须为 `MANUAL_ONLY`，`max_attempts` 固定为 1。
- 对 `MANUAL_ONLY` 节点，worker 一旦进入 `CALL_STARTED` 后崩溃，租约过期不得自动重试，只能转为 `FAILED_AMBIGUOUS` 并告警。

状态不明处理：

```text
PENDING -> LOCKED -> CALL_PREPARED -> CALL_STARTED -> SUCCEEDED
                                      -> FAILED_RETRYABLE
                                      -> FAILED_FINAL
                                      -> TIMEOUT
                                      -> FAILED_AMBIGUOUS
```

- `CALL_PREPARED` 表示请求参数、幂等键、payloadRef 已固化，但尚未开始外部调用。
- `CALL_STARTED` 表示 worker 已经把请求发出或已经开始不可回滚副作用。
- 如果 `CALL_PREPARED` 的租约过期，可以释放回 `PENDING`。
- 如果 `CALL_STARTED` 的租约过期，只有 `external_call_policy = IDEMPOTENT_RETRY` 时才能重新投递。
- 如果 `external_call_policy = QUERY_THEN_RETRY`，必须先进入 `RECONCILING` 查询下游状态，确认未执行后才允许重试。
- 如果无法确认下游状态，进入 `FAILED_AMBIGUOUS`，由人工或补偿任务处理，不自动恢复 Flowable。
- `FAILED_AMBIGUOUS` 不等同于业务失败，前端必须显示“状态不明”，不能走普通 `error` 端口继续流程。

### 7.8 等待订阅和早到事件

为解决“回调早于 wait.message 持久化”的竞态，新增 `hb_flow_wait_subscription`：

```sql
CREATE TABLE hb_flow_wait_subscription (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  run_id BIGINT UNSIGNED NOT NULL,
  flow_version_id BIGINT UNSIGNED NOT NULL,
  node_id VARCHAR(128) NOT NULL,
  attempt_no INT UNSIGNED NOT NULL DEFAULT 1,
  wait_seq INT UNSIGNED NOT NULL DEFAULT 1,
  wait_instance_id VARCHAR(128) NOT NULL,
  engine_instance_id VARCHAR(128) NOT NULL,
  execution_id VARCHAR(128) NOT NULL,
  message_name VARCHAR(128) NOT NULL,
  correlation_key VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  timeout_at DATETIME(3) NULL,
  consumed_inbox_id BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_flow_wait_execution (tenant_id, execution_id),
  UNIQUE KEY uk_flow_wait_instance (tenant_id, wait_instance_id),
  KEY idx_flow_wait_match (tenant_id, message_name, correlation_key, status),
  KEY idx_flow_wait_run (tenant_id, run_id, node_id)
);
```

状态：

```text
WAITING
RESUME_SCHEDULED
RESUMED
TIMED_OUT
CANCELED
```

Inbox 需要支持这些状态：

```text
RECEIVED
EARLY_ARRIVED
PROCESSING
PROCESSED
FAILED_RETRYABLE
FAILED_FINAL
EXPIRED
```

当外部事件找不到等待订阅时，不丢弃事件，写为 `EARLY_ARRIVED`。等待订阅创建并提交后，由 `FlowResumeDispatcher` 反查并认领匹配的早到 Inbox。

Correlation Key 规则：

- `correlation_key` 必须具备节点执行级唯一性，不能只使用 `businessKey`、`orderNo` 等业务主键。
- 默认生成规则：

```text
correlationKey = businessKey + ':' + nodeId + ':' + attemptNo + ':' + waitSeq
waitInstanceId = runId + ':' + nodeId + ':' + attemptNo + ':' + waitSeq
```

- 对外请求必须把 `waitInstanceId` 或由它签名得到的 `waitToken` 传给下游，用于回调时精确匹配。
- 如果下游只能原样回传业务主键，必须由节点配置声明 `weakCorrelation=true`，并禁用循环等待自动匹配；此类节点默认进入人工确认或业务自定义消歧逻辑。
- 循环、重试、补偿再次进入同一个 `wait.message` 时，必须递增 `attempt_no` 或 `wait_seq`，禁止复用上一轮 correlation。

Inbox 需要补充字段：

```sql
ALTER TABLE hb_flow_inbox
  ADD COLUMN wait_instance_id VARCHAR(128) NULL,
  ADD COLUMN correlation_hash BIGINT UNSIGNED NULL,
  ADD COLUMN claim_token VARCHAR(64) NULL,
  ADD COLUMN claimed_at DATETIME(3) NULL,
  ADD COLUMN expire_at DATETIME(3) NULL,
  ADD COLUMN archived_at DATETIME(3) NULL,
  ADD COLUMN status_version INT UNSIGNED NOT NULL DEFAULT 0;

CREATE INDEX idx_flow_inbox_early_match
ON hb_flow_inbox (tenant_id, message_name, correlation_key, status, expire_at, id);

CREATE INDEX idx_flow_inbox_wait_instance
ON hb_flow_inbox (tenant_id, wait_instance_id, status);
```

`correlation_hash` 用于高并发分片扫描和降低热点页集中；精确匹配仍以 `tenant_id + message_name + correlation_key/wait_instance_id` 为准。

## 8. Flow DSL 到 BPMN 的编译规则

### 8.1 编译入口

新增：

```java
public interface FlowBpmnCompiler {
    FlowBpmnCompileResult compile(FlowDefinition flow,
                                  List<NodeComponentManifest> manifests);
}
```

返回：

```java
public class FlowBpmnCompileResult {
    private boolean valid;
    private String processKey;
    private String processName;
    private String bpmnXml;
    private String bpmnSha256;
    private List<FlowBpmnElementMapping> mappings;
    private List<FlowValidationIssue> issues;
}
```

### 8.2 BPMN process key

生成规则：

```text
processKey = tenantCode + "_" + flow.code
```

如果 `flow.code = order_paid_sync`，租户编码为 `t_1001`，则：

```text
t_1001_order_paid_sync
```

Flowable 部署时同时传入 `tenantId`。

### 8.3 BPMN element id

每个 Flow node 编译为稳定 BPMN element id：

```text
flow_<flowVersionNo>__node_<flowNodeId>
```

示例：

```text
flow_3__node_http_1
```

要求：

- 同一个发布版本内稳定。
- 不直接使用用户输入的中文名称。
- 不含空格和特殊字符。

### 8.4 节点映射

| Flow 组件 | BPMN 元素 | 说明 |
| --- | --- | --- |
| `trigger.manual` | `startEvent` | 手动 API 调用启动 |
| `trigger.webhook` | `startEvent` | Webhook Controller 校验后启动 |
| `source.mq.consume` | `startEvent` | MQ listener 收到消息后启动，不使用 Flowable 原生 MQ |
| `trigger.cron` | `startEvent` + Quartz/Flowable Timer 二选一 | 首期推荐 Quartz 调用启动，避免两套定时配置混乱 |
| `action.http.request` | `serviceTask` + `wait.message`，或 Flowable External Worker Task | 默认外部化执行 HTTP I/O |
| `source.mysql.query` | `serviceTask` + `wait.message`，或 Flowable External Worker Task | 默认外部化执行 MySQL I/O |
| `source.redis.get` | `serviceTask` + `wait.message`，或 Flowable External Worker Task | 默认外部化执行 Redis I/O |
| `sink.redis.set` | `serviceTask` + `wait.message`，或 Flowable External Worker Task | 默认外部化执行 Redis I/O |
| `sink.mq.publish` | `serviceTask` + `wait.message`，或 Flowable External Worker Task | 默认写 Outbox，由外部投递器确认后恢复 |
| `transform.mapper` | `serviceTask` | delegate 做字段映射 |
| `logic.condition` | `exclusiveGateway` | 根据 expression 编译 sequenceFlow 条件 |
| `system.log` | `serviceTask` | delegate 记录日志 |
| `system.end` | `endEvent` | 流程终点 |
| `approval.userTask` | `userTask` | Flowable 人工任务 |
| `wait.message` | `intermediateCatchEvent` 或 `receiveTask` | 等待外部事件恢复 |
| `wait.timer` | `intermediateCatchEvent timerEventDefinition` | 定时等待 |

节点执行模式强制规则：

| 执行模式 | 适用节点 | BPMN 编译 | 线程池归属 |
| --- | --- | --- | --- |
| `INLINE_CPU` | mapper、log、轻量变量处理 | 同步 `serviceTask` | Flowable 当前推进线程 |
| `ASYNC_SHORT` | 明确小于平台短超时的轻 I/O 或短副作用 | `flowable:async="true"`，`flowable:exclusive="false"` | Flowable Async Executor |
| `EXTERNAL_IO` | HTTP、MySQL 查询、Redis、MQ 投递、第三方 API、文件/导出等可能阻塞节点 | 创建 `hb_flow_io_command` 后进入 `wait.message`，或使用 Flowable External Worker Task | HeartBeat 独立 I/O worker 线程池 |

`flowable:async="true"` 不是耗时 I/O 解耦方案，只允许用于短任务排队和 Flowable 自身 Job 重试。所有可能被下游慢调用拖住的节点，默认必须使用 `EXTERNAL_IO`。

`EXTERNAL_IO` 默认编译形态：

```text
io_prepare_http_1(serviceTask, INLINE_CPU)
  -> wait_http_1_completed(wait.message, messageName = "io.node.completed")
  -> io_result_http_1(serviceTask, INLINE_CPU)
```

其中：

- `io_prepare_*` 只写 `hb_flow_io_command` 和必要的 `payloadRef`，不得发起远程调用。
- 独立 I/O worker 根据 `worker_topic` 消费命令并执行远程调用。
- worker 完成后写 Inbox 事件，事件名为 `io.node.completed`，`correlationKey` 为命令表中的 `correlation_key`。
- `io_result_*` 只负责读取 Inbox payload、写受控变量和决定 `selectedPort`。
- 外部工作者线程池按类型隔离，例如 `flow-io-http`、`flow-io-db`、`flow-io-mq`，每类必须有并发上限、队列上限、超时、熔断和限速。
- Flowable Async Executor 只负责引擎 Job、定时器和短任务，不承担下游网络抖动风险。

如果实现阶段确认当前 Flowable 版本的 External Worker Task 能满足租户、重试、监控和测试要求，可以把 `EXTERNAL_IO` 编译为 Flowable external worker；否则使用 `serviceTask + wait.message + hb_flow_io_command` 作为兼容基线。

### 8.5 连线映射

`FlowEdge`：

```json
{
  "source": "condition_1",
  "sourcePort": "true",
  "target": "http_1",
  "targetPort": "in"
}
```

编译为 BPMN `sequenceFlow`：

```xml
<sequenceFlow id="edge_condition_1_true_http_1"
              sourceRef="flow_3__node_condition_1"
              targetRef="flow_3__node_http_1">
  <conditionExpression xsi:type="tFormalExpression">
    ${flowConditionEvaluator.matches(execution, 'condition_1', 'true')}
  </conditionExpression>
</sequenceFlow>
```

普通节点只有 `out` 端口时，sequenceFlow 不需要条件。

多输出端口规则：

- `logic.condition` 的 `true` / `false` 由 `FlowConditionEvaluator` 基于已发布的表达式 AST 决定。
- 普通 serviceTask 如果有多个输出端口，delegate 执行后写入变量 `selectedPort`，后续 sequenceFlow 使用条件表达式。
- `error` 端口优先映射为边界错误事件；首期可以退化为 delegate 设置 `selectedPort = 'error'` 后走普通 sequenceFlow。

### 8.6 表达式策略

首期不允许用户写任意 SpEL/Java 表达式。

允许的表达式格式：

```text
payload.status == 'PAID'
payload.amount > 100
payload.type in ['A','B']
```

编译策略：

- HeartBeat 自己解析表达式。
- 用户表达式先解析成 HeartBeat AST，并以 AST JSON 存入 `hb_flow_engine_mapping` 或版本扩展字段。
- BPMN sequenceFlow 中只生成受控 UEL：`${flowConditionEvaluator.matches(execution, '<nodeId>', '<port>')}`。
- 用户表达式本身不直接翻译成业务 UEL。
- 用户表达式不允许调用 Bean。
- 用户表达式不允许调用静态方法。
- 用户表达式不允许访问 classloader、system properties、environment。
- `flowConditionEvaluator` 是编译器生成的唯一白名单内部 Bean 调用，只能读取受控变量、表达式 AST 和节点映射。
- evaluator 的空值、数组、数字比较、字符串比较、布尔短路语义必须由 HeartBeat 自己定义，不依赖 Flowable UEL 的隐式类型转换。

编译期沙盒验证：

```text
POST /api/v1/flows/{id}/compile
```

请求可以携带 mock payload：

```json
{
  "samplePayloads": [
    {"amount": 0, "type": "A"},
    {"amount": 101, "type": "B"},
    {"amount": null, "type": "C"}
  ],
  "strictExpressionCheck": true
}
```

如果调用方未提供 `samplePayloads`，后端根据节点输入 JSON Schema 生成最小边界样本：

- 缺失字段。
- 字段为 `null`。
- 数字边界值，例如 0、阈值、阈值上下 1。
- 空字符串和普通字符串。
- 空数组、单元素数组、多元素数组。
- 枚举值和未知值。

每个条件表达式必须通过：

1. DSL 语法解析。
2. AST 类型检查。
3. 沙盒 AST 解释执行。
4. BPMN 条件 UEL 调用 `FlowConditionEvaluator` 的沙盒执行。
5. AST 结果与 evaluator 结果一致性校验。
6. 空值、缺失字段、数组 contains、短路逻辑边界校验。

任何样本抛出异常、出现 AST/evaluator 结果不一致、或所有出口都无法命中且没有 default edge，编译失败。失败信息必须映射到 Flow nodeId 和表达式文本，不暴露 Flowable UEL 异常。

推荐变量结构：

```json
{
  "payload": {},
  "context": {
    "tenantId": "1",
    "flowId": "100",
    "runId": "200",
    "triggerType": "WEBHOOK"
  },
  "nodeOutputs": {
    "node_1": {}
  },
  "selectedPort": "out"
}
```

## 9. 发布流程

### 9.1 保存草稿

沿用现有：

```text
POST /api/v1/flows
PUT  /api/v1/flows/{id}/draft
```

只保存 Flow DSL，不部署 Flowable。

### 9.2 编译校验

```text
POST /api/v1/flows/{id}/compile
```

执行：

1. 读取 Flow DSL。
2. 读取 active node component manifests。
3. `FlowDslValidator.validate(...)`。
4. `FlowExpressionSandbox.validate(...)`，用调用方 mock payload 或 schema 生成样本验证所有条件表达式。
5. `FlowBpmnCompiler.compile(...)`。
6. `FlowBpmnCompileVerifier.verify(...)`，沙盒执行 BPMN 条件 UEL 的受控 evaluator 调用。
7. 返回 BPMN XML 摘要、节点映射、表达式样本覆盖结果、错误列表。

校验项：

- 至少一个触发节点。
- 至少一个结束节点。
- 所有节点组件存在且 active。
- 所有 edge 端口存在。
- 不允许多个无条件入口。
- 不允许发布含孤立节点的流程，除非节点标记为 disabled。
- 不允许用户任务缺少 assignee/candidate 策略。
- 不允许生产态节点使用 mock executor。
- 不允许节点 config 包含 secret 原文。
- 不允许条件表达式未通过 AST 类型检查和沙盒执行。
- 不允许 gateway 在样本和 schema 边界下全部无法命中，除非配置 default edge。
- 不允许长耗时 I/O 节点使用 `INLINE_CPU` 或仅使用 `flowable:async` 作为执行模式。
- 不允许 `EXTERNAL_IO` 节点缺少外部幂等声明：`supportsExternalIdempotency`、`externalCallPolicy`、`idempotencyHeaderName`。
- 如果 `supportsExternalIdempotency=false` 且没有 `statusQuery` 能力，编译器必须强制 `externalCallPolicy=MANUAL_ONLY`、`maxAttempts=1`，否则拒绝发布。
- 如果节点位于循环、补偿或可重试路径中，必须生成执行级 `waitInstanceId` 和 correlation，不允许只使用业务主键作为 correlation。

### 9.3 发布版本

```text
POST /api/v1/flows/{id}/publish
```

执行顺序：

1. 开启本地事务。
2. 读取 FlowDefinition。
3. 编译 Flow DSL 为 BPMN XML。
4. 计算 `flow_dsl_sha256` 和 `bpmn_sha256`。
5. 写入 `hb_flow_version`，状态 `PUBLISHING`。
6. 调用 `FlowableDeploymentService.deploy(...)`。
7. 获取 `deploymentId` 和 `processDefinitionId`。
8. 更新 `hb_flow_version` 为 `PUBLISHED`。
9. 更新 `hb_flow_definition.active_version_no`、`active_process_definition_id`、`active_deployment_id`。
10. 写入 `hb_flow_engine_mapping`。
11. 提交事务。

注意：

- Flowable deploy 与业务库写入在同一数据库时，可以放在同一本地事务内。
- 如果部署失败，`hb_flow_version` 标记为 `FAILED`，保存 `compile_error`。
- 已发布版本不可覆盖，重新发布必须生成新版本号。

## 10. 触发器设计

### 10.1 统一触发命令

```java
public class FlowTriggerCommand {
    private Long tenantId;
    private Long flowId;
    private Long flowVersionId;
    private Long triggerId;
    private String triggerType;
    private String triggerKey;
    private String businessKey;
    private String idempotencyKey;
    private String correlationKey;
    private Map<String, Object> payload;
    private Map<String, Object> headers;
    private Instant triggeredAt;
}
```

所有入口先转成这个命令，再调用：

```java
FlowRunDTO trigger(FlowTriggerCommand command);
```

### 10.2 手动触发

API：

```text
POST /api/v1/flows/{id}/run
```

请求：

```json
{
  "businessKey": "order-10001",
  "idempotencyKey": "manual-order-10001-1",
  "payload": {
    "orderNo": "10001",
    "status": "PAID"
  }
}
```

处理：

1. 校验 `flow:run:start` 权限。
2. 读取当前 active version。
3. 如果未发布，拒绝启动。
4. 如果 `idempotencyKey` 已存在，返回已有 run。
5. 创建 `hb_flow_run`，状态 `CREATED`。
6. 调用 Flowable `RuntimeService.startProcessInstanceById(...)`。
7. 更新 `engine_instance_id`，状态 `RUNNING`。

### 10.3 Webhook 触发

API：

```text
POST /api/v1/flow-webhooks/{webhookKey}
```

安全要求：

- `webhookKey` 必须随机生成，长度不少于 32。
- 支持签名：`X-Heartbeat-Signature`。
- 支持时间戳：`X-Heartbeat-Timestamp`。
- 可配置 IP 白名单。
- 请求体最大大小默认 1 MB。
- 原始请求体不直接写日志，只写摘要。

幂等键生成：

```text
idempotencyKey = header['X-Idempotency-Key']
              ?: sha256(webhookKey + rawBody + timestampMinute)
```

### 10.4 Cron 触发

首期建议使用当前 Quartz：

```text
Quartz Job
  -> FlowTriggerService.trigger(...)
  -> Flowable start process
```

不直接使用 Flowable timer start event，原因：

- 项目已经有 Quartz。
- 当前 `sys_job` 体系已有启停、日志和权限。
- 避免 Quartz 和 Flowable Timer 双定时来源。

后续如果要迁移到 Flowable Timer，需要明确停用同一触发器的 Quartz Job。

### 10.5 MQ 触发

RocketMQ/Kafka listener 收到消息：

```text
message
  -> FlowMqTriggerListener
  -> 匹配 hb_flow_trigger
  -> Inbox 幂等
  -> FlowTriggerService.trigger(...)
```

幂等键：

```text
RocketMQ: topic + messageId
Kafka: topic + partition + offset
```

要求：

- 消费者先写 Inbox。
- 重复消息不得重复启动流程。
- Flow 启动成功后提交 MQ ack。
- Flow 启动失败时按 MQ 重试策略重投。

### 10.6 领域事件触发

现有 `DomainEventPublisher` 可以继续发布 Spring 进程内事件。但生产可靠事件必须走 Outbox：

```text
业务事务
  -> 写业务表
  -> 写 sys_outbox_event
  -> OutboxPublisher 投递 MQ
  -> FlowDomainEventConsumer
  -> Inbox 幂等
  -> FlowTriggerService.trigger(...)
```

事件主题命名：

```text
domain.user.registered
domain.pay.order.paid
domain.workflow.task.completed
```

## 11. 运行流程

### 11.1 启动流程

伪代码：

```java
@Transactional
public FlowRunDTO trigger(FlowTriggerCommand command) {
    FlowRun existing = flowRunRepository.findByIdempotencyKey(command.getTenantId(), command.getIdempotencyKey());
    if (existing != null) {
        return assembler.toDto(existing);
    }

    FlowVersion version = flowRepository.findActiveVersion(command.getFlowId());
    assertPublished(version);

    FlowRun run = flowRunRepository.createCreatedRun(command, version);

    Map<String, Object> variables = variableCodec.startVariables(command, run, version);
    ProcessInstance instance = runtimeService.startProcessInstanceById(
        version.getProcessDefinitionId(),
        command.getBusinessKey(),
        variables,
        String.valueOf(command.getTenantId())
    );

    flowRunRepository.markRunning(run.getId(), instance.getProcessInstanceId());
    flowRunEventRepository.appendFlowStarted(run, instance);
    return assembler.toDto(run);
}
```

### 11.2 ServiceTask 执行

所有自动节点统一走：

```xml
<serviceTask id="flow_3__node_http_1"
             name="HTTP 请求"
             flowable:delegateExpression="${flowableNodeDelegate}" />
```

delegate：

```java
public class FlowableNodeDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        FlowBpmnElementMapping mapping = mappingRepository.findByBpmnElementId(activityId);
        FlowNode node = flowVersionRepository.findNode(mapping.getFlowVersionId(), mapping.getFlowNodeId());
        NodeExecutor executor = executorRegistry.get(mapping.getExecutorId());

        NodeExecutionCommand command = commandFactory.from(execution, node, mapping);
        NodeExecutionOutcome outcome = executor.execute(command);

        variableCodec.applyOutcome(execution, node, outcome);
    }
}
```

约束：

- `FlowableNodeDelegate` 只负责调用节点执行器和写回 Flowable 受控变量。
- `FlowableNodeDelegate` 不作为最终运行轨迹投影的唯一来源。
- `hb_flow_run_event` 的标准投影由 `FlowableProjectionEventListener` 统一处理。
- delegate 可以写入调试级别的本地日志，但不得绕过监听器直接制造与 Flowable 状态不一致的最终事件。
- 如果 delegate 捕获节点业务错误，应通过 `NodeExecutionOutcome` 写入受控变量，由监听器统一落库。
- 对 `EXTERNAL_IO` 节点，delegate 不执行远程调用，只创建 `hb_flow_io_command`、写入 correlation 变量并让流程进入等待节点。

### 11.3 I/O 节点外部工作者模式

`EXTERNAL_IO` 节点运行链路：

```text
Flowable engine thread
  -> io_prepare serviceTask
  -> FlowableNodeDelegate
  -> 创建 hb_flow_io_command(PENDING)
  -> 写变量 ioCommandId/ioCorrelationKey/waitInstanceId
  -> 提交事务
  -> 流程进入 wait.message

FlowExternalIoCommandDispatcher
  -> 按 worker_topic 拉取 PENDING command
  -> 固化请求参数和外部幂等键
  -> 独立线程池执行 HTTP/MySQL/Redis/MQ
  -> 大结果写 FlowablePayloadStore
  -> 写 Inbox(io.node.completed, correlationKey)
  -> FlowResumeService.resume(...)
  -> Flowable messageEventReceived(...)
```

隔离要求：

- HTTP、DB、MQ、文件/导出必须使用不同 worker topic 和不同线程池。
- 每个线程池必须有最大并发、最大队列、单任务超时和租户级限速。
- 下游超时或熔断只影响对应 topic，不阻塞 Flowable Async Executor。
- worker 不得直接调用 `RuntimeService.messageEventReceived` 绕过 Inbox。
- worker 完成事件即使早于 wait.message 持久化，也必须写 Inbox，交给早到事件机制处理。
- `hb_flow_io_command` 的 retry、timeout、cancel 状态是 I/O 层真相来源；Flowable 只等待最终完成/失败/超时事件。
- worker 发出外部请求前必须把 command 更新为 `CALL_STARTED`，并记录 `call_started_at`。
- `CALL_STARTED` 之后发生 Pod 驱逐、OOM、进程崩溃或锁租约过期，不能按普通锁超时直接回到 `PENDING`。
- 所有外部请求必须携带 `idempotency_key`，除非节点 manifest 明确声明 `supportsExternalIdempotency=false` 且 `external_call_policy=MANUAL_ONLY`。
- 对不支持幂等且不可查询状态的下游，`CALL_STARTED` 后失败只能进入 `FAILED_AMBIGUOUS`。

结果恢复：

- 成功：Inbox payload 包含 `outputPayloadRef`、`summary`、`selectedPort=out`。
- 可重试失败：更新 command 为 `FAILED_RETRYABLE`，由 dispatcher 计算 `next_attempt_at`。
- 最终失败：写 Inbox 事件 `io.node.completed`，payload 中携带 `selectedPort=error`、`errorCode`、`errorMessage`。
- 超时：写 Inbox 事件，`selectedPort=timeout`。
- 状态不明：不写普通完成 Inbox，不恢复 Flowable，写 `IO_COMMAND_AMBIGUOUS` 运行事件并进入人工处理。

worker 租约回收规则：

| 当前状态 | 租约过期处理 | 原因 |
| --- | --- | --- |
| `LOCKED` | 可释放回 `PENDING` | 尚未发起外部副作用 |
| `CALL_PREPARED` | 可释放回 `PENDING` | 请求已固化但未发出 |
| `CALL_STARTED` + `IDEMPOTENT_RETRY` | 可重新投递，必须复用同一 `idempotency_key` | 下游能识别重复请求 |
| `CALL_STARTED` + `QUERY_THEN_RETRY` | 进入 `RECONCILING`，先查下游状态 | 下游支持状态确认 |
| `CALL_STARTED` + `MANUAL_ONLY` | 进入 `FAILED_AMBIGUOUS` | 下游不支持幂等且无法确认状态 |

Kubernetes 约束：

- worker Pod 必须实现 graceful shutdown：收到 SIGTERM 后停止领取新命令，只等待当前命令完成或到达本地 drain 超时。
- `terminationGracePeriodSeconds` 必须大于平台允许的单次 I/O 超时加上结果落库时间。
- 这些设置只能降低中断概率，不能替代外部幂等；强杀、节点故障、OOM 仍按状态不明规则处理。

### 11.4 节点输出变量

每个节点输出写入：

```text
nodeOutputs.<nodeId> = output
lastOutput = output
selectedPort = outcome.selectedPort
selectedPorts.<nodeId> = outcome.selectedPorts
```

默认只写瘦变量，不写完整大 payload。

示例：

```json
{
  "nodeOutputs": {
    "mysql_1": {
      "rows": [
        {"id": 1, "status": "PAID"}
      ]
    }
  },
  "lastOutput": {
    "rows": [
      {"id": 1, "status": "PAID"}
    ]
  },
  "selectedPorts": {
    "condition_1": ["true"]
  }
}
```

瘦 Payload 规则：

- Flowable 变量只保存流程路由必需数据、业务主键、摘要和外部 payload 引用。
- 单个变量默认不超过 8 KB，单个流程实例所有 HeartBeat 自定义变量默认不超过 64 KB。
- 超过阈值的节点输出写入 `FlowablePayloadStore`，Flowable 变量只保存引用。
- 查询结果集、长文本、文件内容、第三方原始响应不得直接写入 `ACT_RU_VARIABLE`。
- `ACT_GE_BYTEARRAY` 不作为业务 payload 存储使用。
- 节点执行器需要完整数据时，通过 `payloadRef` 从 HeartBeat 自有存储读取。

引用格式：

```json
{
  "payloadRef": {
    "storage": "HB_FLOW_PAYLOAD",
    "id": "90001",
    "sha256": "8d0b...",
    "summary": {
      "type": "array",
      "rowCount": 200,
      "truncated": true
    }
  }
}
```

### 11.5 网关推进

Flow DSL：

```text
condition_1.true  -> http_1.in
condition_1.false -> end_1.in
```

BPMN：

```xml
<exclusiveGateway id="flow_3__node_condition_1" />

<sequenceFlow id="edge_condition_1_true_http_1"
              sourceRef="flow_3__node_condition_1"
              targetRef="flow_3__node_http_1">
  <conditionExpression xsi:type="tFormalExpression">
    ${flowConditionEvaluator.matches(execution, 'condition_1', 'true')}
  </conditionExpression>
</sequenceFlow>
```

如果没有任何条件命中：

- 如果存在 default edge，走 default。
- 如果不存在 default edge，流程失败，错误码 `FLOW_NO_ROUTE_MATCHED`。

### 11.6 结束流程

进入 `system.end` 编译出的 `endEvent` 后：

- Flowable 结束实例。
- `FlowableEventBridge` 或历史同步器将 `hb_flow_run.status` 更新为 `SUCCESS`。
- 写入 `FLOW_COMPLETED` 事件。
- 保存 `outputSummary = lastOutput`。

### 11.7 运行投影一致性

`hb_flow_run` 和 `hb_flow_run_event` 是业务展示投影，不是真相来源。真相来源是 Flowable runtime/history 表。

投影必须遵守：

- 标准投影由 `FlowableProjectionEventListener` 集中完成。
- 监听 Flowable 事件：`PROCESS_STARTED`、`ACTIVITY_STARTED`、`ACTIVITY_COMPLETED`、`ACTIVITY_CANCELLED`、`JOB_EXECUTION_FAILURE`、`JOB_RETRIES_DECREMENTED`、`TASK_CREATED`、`TASK_COMPLETED`、`PROCESS_COMPLETED`、`PROCESS_CANCELLED`。
- 当前阶段默认使用 `LOCAL_TX` 投影模式：监听器与 Flowable 引擎推进处于同一个 Spring 事务。
- `LOCAL_TX` 模式下，监听器写投影失败时，当前 Flowable 推进事务必须回滚。
- 投影表必须支持按 `engineInstanceId + activityId + eventType + attemptNo` 幂等写入。
- 定时补偿任务只允许从 Flowable 历史表重建 HeartBeat 投影，不允许反向修改 Flowable 状态。
- 代码层必须通过 `FlowProjectionPublisher` 接口写投影，不允许业务代码直接绑定具体同步实现。

推荐监听器：

```java
@Component
public class FlowableProjectionEventListener implements FlowableEventListener {
    private final FlowProjectionPublisher projectionPublisher;

    @Override
    @Transactional
    public void onEvent(FlowableEvent event) {
        FlowProjectionEvent projection = projectionMapper.from(event);
        projectionPublisher.publish(projection);
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }
}
```

投影发布接口：

```java
public interface FlowProjectionPublisher {
    void publish(FlowProjectionEvent event);
}
```

实现策略：

| 模式 | 实现类 | 适用场景 | 一致性 |
| --- | --- | --- | --- |
| `LOCAL_TX` | `LocalTransactionProjectionPublisher` | Flowable 表和 HeartBeat 表同库 | 本地事务强一致 |
| `ASYNC_OUTBOX` | `OutboxProjectionPublisher` | Flowable 引擎库拆分到独立库 | 最终一致 |
| `MQ_PROJECTION` | `MqProjectionPublisher` | 投影消费者独立扩缩容 | 最终一致 |

拆库演进预案：

- Flowable 引擎库独立后，监听器不能直接写 HeartBeat 业务库。
- 监听器只在引擎库本地事务内写 `projection_outbox` 或发送可靠 MQ 事件。
- 投影消费者从 outbox/MQ 读取 `FlowProjectionEvent`，幂等更新 `hb_flow_run` 和 `hb_flow_run_event`。
- 每个投影事件必须有 `projection_event_id`、`engine_instance_id`、`activity_id`、`event_type`、`event_time` 和单调 `engine_event_seq`。
- `FlowProjectionReconcileJob` 必须支持按 processInstanceId、时间窗口和 event seq 从 Flowable 历史表补投影。
- 前端必须把投影标记为 `PROJECTION_SYNCING`、`PROJECTION_DELAYED` 或 `PROJECTION_CONSISTENT`，避免把最终一致延迟误判为流程异常。

补偿任务：

```text
FlowProjectionReconcileJob
  -> 查询最近 N 分钟 Flowable ACT_HI_* 历史
  -> 对比 hb_flow_run / hb_flow_run_event
  -> 只补缺失投影
  -> 记录 reconcile 审计日志
```

该任务用于修复显示投影，不参与流程推进。

## 12. 节点执行器契约

### 12.1 新接口

当前 `NodeExecutor` 偏 debug。生产态建议扩展为：

```java
public interface NodeExecutor {
    String executorId();

    NodeExecutionOutcome execute(NodeExecutionCommand command);
}
```

命令：

```java
public class NodeExecutionCommand {
    private String runId;
    private String engineInstanceId;
    private String nodeId;
    private String nodeType;
    private Map<String, Object> nodeConfig;
    private Map<String, Object> input;
    private Map<String, Object> variables;
    private Long tenantId;
    private String businessKey;
    private int attemptNo;
}
```

结果：

```java
public class NodeExecutionOutcome {
    private String status;
    private Map<String, Object> output;
    private List<String> selectedPorts;
    private boolean waitRequired;
    private FlowWaitRequest waitRequest;
    private boolean retryable;
    private String errorCode;
    private String errorMessage;
}
```

### 12.2 错误分类

| 类型 | 表现 | 处理 |
| --- | --- | --- |
| 业务分支错误 | HTTP 404、条件不满足、业务状态不允许 | 不抛异常，返回 `selectedPorts=['error']` |
| 可重试技术错误 | 网络超时、连接池临时失败、MQ 暂时不可用 | 抛 `RetryableNodeException`，Flowable Job 重试 |
| 不可重试技术错误 | 配置缺失、凭据不存在、SQL 非法 | 抛 `NonRetryableNodeException`，流程失败 |
| 等待型结果 | 人工任务、外部回调、定时等待 | 返回 `waitRequired=true` 或进入对应 BPMN wait element |

### 12.3 凭据规则

节点 config 只能保存：

```json
{
  "connectionId": "10001",
  "url": "/pay/sync",
  "method": "POST"
}
```

不允许：

```json
{
  "password": "123456",
  "accessKey": "xxx",
  "secretKey": "yyy"
}
```

delegate 执行时：

1. 从 node config 取 `connectionId`。
2. 调用 `ConnectionCredentialService`。
3. 校验租户和权限。
4. 解密 secret。
5. 调用 executor。
6. 日志和运行事件中脱敏。

## 13. 人工任务和审批迁移

### 13.1 UserTask 节点

新增组件：

```text
approval.userTask
```

配置：

```json
{
  "taskName": "经理审批",
  "assigneeType": "USER",
  "assigneeExpression": "${payload.managerUserId}",
  "candidateRoleCodes": ["finance_manager"],
  "formSchemaRef": "structure-version-1001",
  "dueDuration": "PT24H"
}
```

编译为：

```xml
<userTask id="flow_3__node_approval_1"
          name="经理审批"
          flowable:assignee="${approvalAssigneeResolver.resolve(execution)}" />
```

候选人通过监听器或任务创建后设置：

```java
taskService.addCandidateUser(taskId, userId);
taskService.addCandidateGroup(taskId, roleCode);
```

### 13.2 与现有 workflow API 兼容

短期：

- `/api/v1/workflow/tasks/todo` 可以继续查旧 `wf_task`。
- 新 Flowable 任务走 `/api/v1/flow-tasks/todo`。

过渡期：

- `FlowableTaskBridge` 将 Flowable UserTask 投影到旧响应格式。
- 前端待办页可以同时展示旧 `wf_task` 和新 Flowable task。

长期：

- 旧 `wf_process_instance`、`wf_task` 不再作为新流程主存储。
- Flowable `ACT_RU_TASK` / `ACT_HI_TASKINST` 是任务真相来源。
- HeartBeat 自己保留 `hb_flow_task_projection` 或使用 `hb_flow_run_event` 展示业务摘要。

### 13.3 审批完成事件

审批完成：

```text
POST /api/v1/flow-tasks/{taskId}/complete
  -> TaskService.complete(taskId, variables)
  -> Flowable 推进
  -> 写 hb_flow_run_event USER_TASK_COMPLETED
  -> 写 sys_outbox_event FLOW_USER_TASK_COMPLETED
```

Outbox payload：

```json
{
  "eventType": "FLOW_USER_TASK_COMPLETED",
  "tenantId": "1",
  "flowRunId": "2001",
  "processInstanceId": "abc",
  "taskId": "task-100",
  "nodeId": "approval_1",
  "action": "APPROVE",
  "operatorId": "10001"
}
```

## 14. 等待和恢复

### 14.1 等待外部事件

适合场景：

- 等待支付回调。
- 等待第三方审批回调。
- 等待设备上报。
- 等待异步导出完成。

Flow DSL 节点：

```text
wait.message
```

配置：

```json
{
  "messageName": "pay.notify.received",
  "correlationKey": "${payload.orderNo}",
  "timeout": "PT30M"
}
```

BPMN：

```xml
<intermediateCatchEvent id="flow_3__node_wait_pay">
  <messageEventDefinition messageRef="msg_pay_notify_received"/>
</intermediateCatchEvent>
```

### 14.2 恢复入口

外部事件到达：

```text
Webhook/MQ/DomainEvent
  -> FlowResumeService.resume(...)
```

命令：

```java
public class FlowResumeCommand {
    private Long tenantId;
    private String eventId;
    private String consumerCode;
    private String messageName;
    private String waitInstanceId;
    private String correlationKey;
    private Map<String, Object> payload;
}
```

处理：

1. Inbox 查重。
2. 写 Inbox `RECEIVED`。
3. 查询 `hb_flow_wait_subscription` 中 `messageName + waitInstanceId/correlationKey + WAITING` 的等待订阅。
4. 如果找到等待订阅，使用条件更新原子认领 Inbox，状态改为 `PROCESSING`。
5. 如果条件更新影响行数为 0，说明消息已被其他实例认领、已过期或状态已变化，当前线程退出并重新读取最终状态。
6. 调用 Flowable：

```java
runtimeService.messageEventReceived(messageName, executionId, variables);
```

7. 更新 Inbox `PROCESSED`，等待订阅更新为 `RESUMED`。
8. 写 `EXTERNAL_EVENT_RECEIVED` 运行事件。
9. 如果没有找到等待订阅，不返回失败，不丢弃事件，Inbox 更新为 `EARLY_ARRIVED`。

推荐认领 SQL：

```sql
UPDATE hb_flow_inbox
SET status = 'PROCESSING',
    claim_token = ?,
    claimed_at = NOW(3),
    status_version = status_version + 1,
    updated_at = NOW(3)
WHERE tenant_id = ?
  AND message_name = ?
  AND wait_instance_id = ?
  AND status IN ('RECEIVED', 'EARLY_ARRIVED')
  AND (expire_at IS NULL OR expire_at > NOW(3))
ORDER BY id
LIMIT 1;
```

兼容旧回调没有 `wait_instance_id` 时，才降级使用 `correlation_key`：

```sql
UPDATE hb_flow_inbox
SET status = 'PROCESSING',
    claim_token = ?,
    claimed_at = NOW(3),
    status_version = status_version + 1,
    updated_at = NOW(3)
WHERE tenant_id = ?
  AND message_name = ?
  AND correlation_key = ?
  AND status IN ('RECEIVED', 'EARLY_ARRIVED')
  AND (expire_at IS NULL OR expire_at > NOW(3))
ORDER BY id
LIMIT 1;
```

批量扫描早到事件时，如果必须先查再处理，可以使用 MySQL 8 的 `FOR UPDATE SKIP LOCKED`，但只用于队列式批处理，不作为单条恢复的主路径：

```sql
SELECT id
FROM hb_flow_inbox
WHERE tenant_id = ?
  AND status = 'EARLY_ARRIVED'
  AND correlation_hash = ?
  AND (expire_at IS NULL OR expire_at > NOW(3))
ORDER BY id
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

早到事件处理：

```text
外部回调到达
  -> Inbox(EARLY_ARRIVED)
  -> 等待节点尚未提交，先保存事件

Flowable 进入 wait.message
  -> FlowWaitRegistrationListener 监听 ACTIVITY_STARTED
  -> 同事务写 hb_flow_wait_subscription(WAITING)
  -> 事务提交后唤醒 FlowResumeDispatcher
  -> Dispatcher 查询 EARLY_ARRIVED Inbox
  -> 认领并调用 messageEventReceived
```

约束：

- `FlowWaitRegistrationListener` 只登记等待订阅，不在 Flowable 当前事务未提交前调用 `messageEventReceived`。
- 早到 Inbox 必须有 TTL；超过等待窗口仍无法匹配时只能软过期为 `EXPIRED` 并告警。
- 同一个 `messageName + correlationKey` 如果匹配多个等待订阅，必须按租户、runId、nodeId 或业务配置消歧；无法消歧时标记 `FAILED_FINAL`，错误码 `FLOW_WAIT_AMBIGUOUS`。
- Inbox 状态从 `EARLY_ARRIVED` 到 `PROCESSING` 必须原子认领，防止多实例重复恢复。
- 热路径禁止物理删除 Inbox。TTL 清理只能通过条件更新执行 `EARLY_ARRIVED -> EXPIRED`。
- `EXPIRED` 数据至少保留一个归档窗口，例如 7 天；只有归档任务可以物理搬迁或删除。
- 如果消费线程认领失败，不得抛系统异常；必须重新读取 Inbox 状态并按 `PROCESSING`、`PROCESSED`、`EXPIRED`、不存在分别处理。

### 14.3 超时

等待节点可以设置边界定时器：

```xml
<boundaryEvent id="wait_pay_timeout" attachedToRef="flow_3__node_wait_pay">
  <timerEventDefinition>
    <timeDuration>PT30M</timeDuration>
  </timerEventDefinition>
</boundaryEvent>
```

超时后走 `timeout` 端口：

```text
wait_pay.timeout -> notify_manual.in
```

## 15. Outbox / Inbox 协作

### 15.1 Outbox 用途

Flow 节点里需要对外发事件时，不直接在业务事务中发 MQ。

示例：`sink.mq.publish`

```text
Flowable serviceTask
  -> FlowableNodeDelegate
  -> MqPublishNodeExecutor
  -> ReliableWorkflowEventService.createOutbox(...)
  -> 当前事务提交
  -> OutboxPublisher 投递 MQ
```

### 15.2 Inbox 用途

所有能触发或恢复流程的外部消息，都必须先 Inbox 幂等：

```text
MQ message
Webhook callback
Domain event
Payment notify
Workflow task completed event
```

唯一键：

```text
consumer_code + event_id
```

Flow 消费者命名：

```text
flow-trigger
flow-resume
flow-domain-event
flow-mq-trigger
```

### 15.3 与 Flowable 事务边界

推荐策略：

- 当前阶段 Flowable 与 HeartBeat 业务表使用同一数据源。
- 当前阶段 Flowable 操作、`hb_flow_run_event` 写入、Inbox 状态更新在同一本地事务内。
- 如果未来 Flowable 引擎表拆分到独立库，投影必须切换为 `ASYNC_OUTBOX` 或 `MQ_PROJECTION` 模式，不再假设跨库本地事务。
- 外部 HTTP、数据库查询、MQ broker 确认等阻塞式 I/O 不得在 Flowable 推进事务内执行。
- Flowable 推进事务内只允许创建 `hb_flow_io_command`、Outbox、Inbox、等待订阅和轻量变量。
- 独立 worker 完成 I/O 后，通过 Inbox 恢复 Flowable，而不是持有原 Flowable 事务等待远程结果。
- `flowable:async="true"` 只用于短任务排队和 Flowable Job 重试，不作为长 I/O 解耦手段。

### 15.4 Inbox 抢占、过期和归档

Inbox 是高并发恢复入口，不能把普通行锁作为主路径。

抢占规则：

- 单条恢复优先使用条件 `UPDATE` 抢占，不先 `SELECT ... FOR UPDATE`。
- 批量队列扫描可以使用 `FOR UPDATE SKIP LOCKED`，但必须限制批量大小并按 `correlation_hash` 分片。
- 所有抢占必须带租户、消息名、执行级 correlation 或 `wait_instance_id`。
- `affected_rows = 1` 才表示抢占成功；否则当前线程必须退出或重新读取状态。

过期规则：

```sql
UPDATE hb_flow_inbox
SET status = 'EXPIRED',
    status_version = status_version + 1,
    updated_at = NOW(3)
WHERE status = 'EARLY_ARRIVED'
  AND expire_at <= NOW(3)
LIMIT 1000;
```

约束：

- TTL 任务禁止 `DELETE FROM hb_flow_inbox ...`。
- 热路径禁止物理删除任何 `RECEIVED`、`EARLY_ARRIVED`、`PROCESSING` 记录。
- `EARLY_ARRIVED -> PROCESSING` 和 `EARLY_ARRIVED -> EXPIRED` 是互斥状态竞争，只能通过条件更新决定胜者。
- 如果 TTL 先把消息更新为 `EXPIRED`，后续等待节点不得再消费它，只能记录 `FLOW_WAIT_EVENT_EXPIRED`。
- 如果消费先把消息更新为 `PROCESSING`，TTL 不得再处理它。
- 归档任务只能处理 `PROCESSED`、`FAILED_FINAL`、`EXPIRED` 且超过保留窗口的数据，例如 7 天。
- 归档可以搬迁到历史表、对象存储或冷库；物理删除必须发生在归档成功之后。

### 15.5 高频队列中间件和状态账本

`hb_flow_io_command` 和 `hb_flow_inbox` 是可靠状态账本，不应承担高频消息队列职责。高并发生产环境必须允许引入 MQ/Redis Stream 等中间件承载瞬时流量、消费组调度和削峰。

核心原则：

- 高频消息先进入队列中间件，MySQL 只保存幂等账本、状态快照、审计记录和补偿依据。
- MySQL 不作为高吞吐流转队列使用，不允许通过无限扫描 `hb_flow_io_command` / `hb_flow_inbox` 来硬扛洪峰。
- Flowable 的流程推进仍以 MySQL 中的幂等状态为准；中间件负责吞吐，不负责替代流程状态真相。
- 强一致、高风险业务事件必须在“队列 ack 前”完成 MySQL 幂等落库，或具备可重放来源，避免中间件故障造成状态丢失。

队列模式：

| 模式 | 用途 | 真相来源 | 适用阶段 |
| --- | --- | --- | --- |
| `MYSQL_DIRECT` | 直接写 `hb_flow_io_command` / `hb_flow_inbox` | MySQL | 本地、测试、中低吞吐 |
| `MQ_BUFFER` | 用 Kafka/RocketMQ 承载高吞吐可靠事件流 | MQ 负责流量，MySQL 负责状态账本 | 高频生产推荐 |
| `REDIS_STREAM_BUFFER` | 用 Redis Stream 吸收短时洪峰，drainer 批量落 MySQL | Redis 只是缓冲，MySQL 负责状态账本 | 短时削峰、低风险事件 |

`MYSQL_DIRECT` 约束：

- 所有扫描必须按 `tenant_id + worker_topic/status + next_attempt_at` 或 `tenant_id + message_name + correlation_hash/status` 命中索引。
- 每次扫描必须限制 `LIMIT`，默认不超过 100。
- dispatcher 必须按 topic、tenant、`correlation_hash` 分片，避免单热点页。
- 写入失败或延迟升高时，入口必须触发 backpressure，返回 429 或排队中状态，不允许无限堆积请求线程。
- 压测发现 MySQL 队列表 IOPS、行锁等待或 buffer pool 命中率接近平台阈值时，必须切换到 `MQ_BUFFER` 或 `REDIS_STREAM_BUFFER`，不得继续扩大 MySQL 扫描线程数。

`MQ_BUFFER` 推荐链路：

```text
Webhook/MQ/External callback
  -> FlowQueueBuffer.publish(...)
  -> Kafka/RocketMQ topic
  -> consumer group
  -> 幂等写 hb_flow_inbox / hb_flow_io_command 状态账本
  -> FlowResumeService / FlowExternalIoCommandDispatcher
```

约束：

- Kafka/RocketMQ message key 必须使用 `tenantId + messageName + waitInstanceId/correlationKey`，保证同一等待实例尽量进入同一分区或有序队列。
- Consumer ack 必须发生在 MySQL 幂等落库成功之后。
- Consumer 重复消费必须依赖 `consumer_code + event_id`、`idempotency_key` 或 `wait_instance_id` 幂等落库。
- MQ backlog、consumer lag、落库失败率必须进入监控和告警。
- MQ 不可用时，入口按事件风险降级：可重放事件返回排队失败并等待重试；不可重放高风险回调优先尝试 MySQL 最小 Inbox 落库；仍失败则返回明确失败，不静默吞掉。
- 对需要严格顺序的同一 businessKey，可以使用 RocketMQ 顺序消息或 Kafka 按 key 分区；顺序保证只在同 key 范围内成立。

Redis Stream 缓冲约束：

- Redis 只做缓冲层和削峰层，不作为流程最终状态来源。
- 对支付、扣款、库存等强一致事件，HTTP 回调只有在 MySQL Inbox 最小记录落库成功后才允许返回业务成功；Redis 可以作为通知或二级缓冲。
- 如果选择“先写 Redis、后批量落 MySQL”的突发模式，必须开启 Redis AOF、主从或集群，并明确该模式只适合可重放来源或低风险事件。
- Redis drainer 批量落库时仍必须使用 Inbox 幂等键，重复消息不得产生重复 run 或重复恢复。
- Redis 不可用时，系统降级到 `MYSQL_DIRECT` 或入口限流；不得静默丢弃回调。

选型规则：

- 高频、可持续洪峰：优先 `MQ_BUFFER`。
- 短时尖峰、可重放、低风险：可用 `REDIS_STREAM_BUFFER`。
- 本地开发、单机部署、中低吞吐：可用 `MYSQL_DIRECT`。
- 支付、扣款、库存、审批回调等高价值事件：必须有 MySQL 幂等账本落库成功记录，不能只存在中间件里。

### 15.6 MySQL 状态账本 IOPS 削峰

即使高频消息已经进入 MQ，MySQL 仍然会承接状态账本写入、幂等核销、运行投影和审计。该层必须按数据热度和写入模式拆分，不能让所有状态变化都以单条同步 SQL 落库。

写入分级：

| 写入类型 | 示例 | 优化方式 | 正确性要求 |
| --- | --- | --- | --- |
| 不可逆副作用检查点 | `CALL_STARTED`、外部扣款请求发出前 | 同步 durable 写入或可靠 MQ 事务事件 | 不允许只写 Redis 或本地内存 |
| 热调度索引 | `PENDING` ready queue、租约、下一次重试时间 | Redis ZSET/Hash/Caffeine | 可重建，不是真相来源 |
| 追加流水 | `hb_flow_run_event` 节点轨迹 | 异步批量刷盘、分区表 | 必须可幂等重放和补偿 |
| 冷数据 | 已结束 run、已归档 event、已完成 command | 按月/租户归档 | 不影响在线调度 |

#### 15.6.1 `hb_flow_run_event` 批量刷盘

`hb_flow_run_event` 是 append-only 运行轨迹表，主要瓶颈是 redo log、索引写入和页分裂。极致高并发环境允许启用批量写入模式。

写入模式：

| 模式 | 行为 | 适用场景 |
| --- | --- | --- |
| `SYNC_INSERT` | listener 同事务单条或小批量写入 | 默认、强一致投影 |
| `ASYNC_BATCH_INSERT` | 事件进入本地有界队列，批量 multi-values insert | 高吞吐回放/审计事件 |
| `MQ_BATCH_INSERT` | 投影事件先进入 MQ，consumer 批量写入 | 拆库或独立投影消费者 |

批量策略：

- 默认批大小：500 条。
- 默认刷盘间隔：100 ms。
- SQL 使用 multi-values：

```sql
INSERT IGNORE INTO hb_flow_run_event (...)
VALUES (...), (...), (...);
```

- 幂等键使用 `tenant_id + run_id + event_seq`。
- `event_seq` 必须由后端在同一 run 内单调生成，不能依赖数据库自增顺序。
- 本地队列必须是有界队列，例如 Disruptor ring buffer 或 `ArrayBlockingQueue`。
- 队列满时必须触发 backpressure 或退回 `SYNC_INSERT`，不得静默丢弃事件。
- 进程优雅停机时必须 drain 队列；强杀导致的缺失事件由 `FlowProjectionReconcileJob` 从 Flowable 历史或投影事件流重建。

边界：

- `ASYNC_BATCH_INSERT` 只适合运行轨迹、回放、审计这类可补偿 append 事件。
- `hb_flow_run.status`、等待恢复、外部副作用检查点不得只依赖本地批量队列。
- 如果业务要求“投影失败必须回滚 Flowable 推进”，必须使用 `SYNC_INSERT` 或 `LOCAL_TX`，不能使用纯本地异步批量。

#### 15.6.2 Redis 热状态索引

Redis 可以作为 `hb_flow_io_command` 的热调度索引，减少 worker 抢任务时对 MySQL 的扫描和行锁竞争。

推荐结构：

```text
flow:io:ready:{workerTopic}          ZSET(commandId, nextAttemptAt)
flow:io:cmd:{commandId}              HASH(status, lockedUntil, attemptNo, workerTopic)
flow:io:lease:{commandId}            STRING(fencingToken, ttl)
flow:wait:index:{waitInstanceId}     HASH(messageName, correlationKey, timeoutAt)
```

worker 抢任务：

```text
ZPOPMIN flow:io:ready:{workerTopic}
  -> SET flow:io:lease:{commandId} fencingToken NX PX leaseMs
  -> 读取 flow:io:cmd:{commandId}
  -> 执行 durable transition
```

约束：

- Redis 热状态是调度索引，不是最终状态账本。
- `LOCKED`、租约续期、下一次扫描时间可以先更新 Redis。
- `CALL_STARTED`、`SUCCEEDED`、`FAILED_FINAL`、`FAILED_AMBIGUOUS` 这类关键状态必须先写 MySQL 或可靠 MQ transition log，再执行外部副作用或 ack。
- Redis 丢失或重启后，系统必须能从 MySQL 中未完成的 command 重新构建 ready ZSET。
- 所有 Redis 更新必须携带 fencing token，防止过期 worker 覆盖新 worker 状态。
- Redis AOF、主从或集群只能降低丢失概率，不能改变“Redis 不是最终真相”的边界。

允许异步写回 MySQL 的字段：

- `locked_by`
- `locked_until`
- worker heartbeat
- 调度统计指标

不允许只异步写回的字段：

- `CALL_STARTED`
- `SUCCEEDED`
- `FAILED_FINAL`
- `FAILED_AMBIGUOUS`
- `MANUAL_REQUIRED`

#### 15.6.3 Inbox 热索引和批量核销

Inbox 的精确核销仍然依赖幂等账本，但等待订阅查找可以通过热索引减少 DB 读。

热索引：

- `FlowWaitRegistrationListener` 创建 `hb_flow_wait_subscription` 时，同步写 Redis wait index。
- 当前 Pod 可写入 Caffeine 本地缓存，TTL 与等待超时一致。
- Bloom filter 可以作为“可能存在等待订阅”的负向过滤器，减少无效 DB 查询，但不能作为恢复依据。

回调处理：

```text
外部事件到达
  -> MQ_BUFFER / Redis Stream / HTTP 入口
  -> 查询 Caffeine / Redis wait index
  -> 命中：进入 FlowResumeService 快路径
  -> 未命中：写入 EARLY_ARRIVED 或投递到延迟扫描
```

约束：

- 高风险回调必须先进入 MQ 或 MySQL Inbox 最小记录，再返回业务成功。
- 本地 Caffeine 命中只能减少 DB 查询，不能绕过 Inbox 幂等。
- Redis wait index 缺失不代表等待不存在，必须允许降级查 MySQL 或进入 `EARLY_ARRIVED`。
- 批量核销可以每 200 ms 拉取最多 100 条，使用 `SKIP LOCKED` 批量认领，再批量更新：

```sql
UPDATE hb_flow_inbox
SET status = 'PROCESSED',
    status_version = status_version + 1,
    updated_at = NOW(3)
WHERE id IN (...);
```

#### 15.6.4 分区、分表和冷数据归档

高并发下，单张超大运行事件表会让索引和 buffer pool 成为瓶颈。日志型数据必须提前规划物理隔离。

建议：

- `hb_flow_run_event` 按月分区或分表，例如 `hb_flow_run_event_YYYYMM`。
- 查询默认只查当前 run 所在月份或归档索引定位到的物理表。
- `hb_flow_io_command` 可按 `tenant_id % 16` 或 `tenant_id % 32` 静态哈希分表。
- 同一租户的 command 尽量落在同一分片，降低跨分片查询。
- 已完成 command、processed inbox、历史 run event 定期搬迁到冷表或对象存储。
- 分片键必须在接口和 repository 层统一封装，业务代码不得手写物理表名。

#### 15.6.5 反压红线

任何优化都不能突破物理上限。入口必须有反压。

监控指标：

- MQ consumer lag。
- Redis ready ZSET 长度。
- `hb_flow_io_command` 未完成数量。
- `hb_flow_inbox` `EARLY_ARRIVED` 和 `PROCESSING` 积压。
- MySQL `Innodb_rows_inserted`、`Innodb_rows_updated`、redo log write latency。
- Hikari 连接池等待时间。
- InnoDB row lock wait。

默认红线：

- 单租户 TPS 超过 50 且未启用 `MQ_BUFFER`：强制限流。
- `hb_flow_io_command.PENDING` 超过 5000：入口限流或暂停拉取 MQ。
- DB 连接池等待超过 500 ms：入口返回 429 或停止消费新消息。
- event batch queue 使用率超过 80%：触发 backpressure 或切换同步降级。

禁止事项：

- 禁止通过增加 MySQL 扫描线程数解决长期积压。
- 禁止把 Redis 作为唯一状态存储。
- 禁止在外部副作用已经开始后，仅凭 Redis 状态恢复执行。
- 禁止单条循环插入 `hb_flow_run_event` 作为高频生产默认实现。

## 16. 失败、重试和补偿

### 16.1 Flowable Job 重试

Flowable Job 重试只用于引擎内短任务、timer、轻量 delegate 和状态推进失败。不得把长耗时网络 I/O 放进 Flowable Async Executor 重试。

对 `ASYNC_SHORT` 节点设置异步：

```xml
<serviceTask id="flow_3__node_short_audit_1"
             flowable:async="true"
             flowable:exclusive="false"
             flowable:delegateExpression="${flowableNodeDelegate}" />
```

重试策略：

```text
R3/PT10S
```

含义：

- 最多 3 次。
- 每次间隔 10 秒。

节点配置可覆盖：

```json
{
  "retry": {
    "maxAttempts": 3,
    "interval": "PT10S"
  },
  "timeout": "PT5S"
}
```

I/O 节点硬约束：

- `action.http.request`、`source.mysql.query`、`source.redis.get`、`sink.redis.set`、`sink.mq.publish` 默认必须使用 `EXTERNAL_IO`。
- 只有被组件 manifest 明确声明为 `shortIo=true` 且平台超时不超过短任务阈值的节点，才允许使用 `ASYNC_SHORT`。
- `ASYNC_SHORT` 默认超时不超过 1 秒，平台上限不超过 3 秒。
- `EXTERNAL_IO` 节点超时由 `hb_flow_io_command.timeout_at` 和 worker 控制，不占用 Flowable 线程等待。
- `NodeExecutor` 发起远程调用时不得开启长事务。
- worker 执行远程调用前只持有 `hb_flow_io_command` 的短锁或租约，不得持有 Flowable Job 锁和 HeartBeat 业务表行锁。
- HTTP 节点必须配置连接超时、读取超时和最大响应体大小。
- MySQL 查询节点必须配置最大行数、查询超时和只读连接。
- MQ publish 节点优先写 Outbox，不在 delegate 中同步阻塞等待 broker 确认。

### 16.2 外部 I/O 命令重试

`EXTERNAL_IO` 重试不使用 Flowable Job retry，而使用 `hb_flow_io_command`：

```json
{
  "retry": {
    "maxAttempts": 3,
    "backoff": "EXPONENTIAL",
    "initialInterval": "PT5S",
    "maxInterval": "PT2M"
  },
  "timeout": "PT30S"
}
```

处理规则：

- worker 拉取命令后设置 `LOCKED` 和 `locked_until`。
- worker 固化请求参数、payloadRef、外部幂等键后设置 `CALL_PREPARED`。
- worker 发出外部请求前必须先提交 `CALL_STARTED` 和 `call_started_at`。
- 远程调用必须携带同一个 `idempotency_key`，重试时不得生成新幂等键。
- 远程调用失败且可重试时，更新为 `FAILED_RETRYABLE`，计算 `next_attempt_at`。
- 超过最大重试次数时，更新为 `FAILED_FINAL`，写 Inbox 完成事件，`selectedPort=error`。
- 超过 `timeout_at` 时，更新为 `TIMEOUT`，写 Inbox 完成事件，`selectedPort=timeout`。
- dispatcher 定期扫描锁过期命令，但必须按状态分支处理：
  - `LOCKED` / `CALL_PREPARED` 可以释放回 `PENDING`。
  - `CALL_STARTED + IDEMPOTENT_RETRY` 可以复用同一幂等键重试。
  - `CALL_STARTED + QUERY_THEN_RETRY` 必须先进入 `RECONCILING` 查询下游结果。
  - `CALL_STARTED + MANUAL_ONLY` 必须进入 `FAILED_AMBIGUOUS`。
- I/O retry 事件写入 `hb_flow_run_event`，但 Flowable execution 仍停留在等待节点，直到收到最终完成/失败/超时事件。
- `FAILED_AMBIGUOUS` 不自动写完成 Inbox，也不推进 Flowable；必须由人工确认或补偿查询转换为 `SUCCEEDED`、`FAILED_FINAL`、`TIMEOUT` 或 `CANCELED`。

状态不明自动对账：

```text
FAILED_AMBIGUOUS
  -> RECONCILING
  -> SUCCEEDED / FAILED_FINAL / TIMEOUT / MANUAL_REQUIRED
```

规则：

- 外部系统接入评审必须优先要求 `statusQuery` 能力，即使不支持幂等写，也要能按 `idempotency_key`、`external_request_id` 或 `businessKey` 查询真实状态。
- `FlowExternalIoReconcileJob` 定时扫描 `FAILED_AMBIGUOUS`，按 `risk_level`、`next_reconcile_at`、租户限速分批探测下游状态。
- 查到下游已成功：写 output payload，命令转 `SUCCEEDED`，写 Inbox 完成事件恢复流程。
- 查到下游明确失败或未执行且已超过安全窗口：命令转 `FAILED_FINAL`，按 `error` 端口或流程失败处理。
- 下游仍不可达或结果含糊：指数退避更新 `next_reconcile_at`，超过平台上限后转 `MANUAL_REQUIRED`。
- `MANUAL_REQUIRED` 才创建人工工单；禁止每条 `FAILED_AMBIGUOUS` 立即生成独立人工工单。
- 人工工单必须按租户、连接、节点、错误类型聚合，避免旧系统抖动时产生工单洪峰。
- 对支付、扣款、库存等高风险节点，如果没有外部幂等或状态查询能力，默认拒绝发布；只有管理员以 `riskAccepted=true` 显式确认后才允许 `MANUAL_ONLY`。

### 16.3 失败状态

技术异常最终失败：

- Flowable Job 进入 deadletter。
- `hb_flow_run.status = FAILED`。
- 写 `NODE_FAILED` 和 `FLOW_FAILED`。
- 前端运行历史显示失败节点、异常摘要、重试次数。

### 16.4 异常翻译

Flowable 原始异常不得直接展示给业务用户。所有运行错误必须经过 `FlowableExceptionTranslator`。

输入：

- Flowable exception class。
- exception message。
- processInstanceId。
- activityId。
- executionId。
- processDefinitionId。

处理：

1. 从异常消息、事件对象或历史活动中提取 BPMN element id。
2. 通过 `hb_flow_engine_mapping` 反查 Flow `nodeId`、组件类型和节点名称。
3. 转换为 HeartBeat 错误码。
4. 保存用户可读错误和技术摘要。
5. 技术堆栈只写后台日志，不直接进入前端响应。

示例：

```text
Flowable 原始错误:
No outgoing sequence flow of the exclusive gateway 'flow_3__node_condition_1' could be selected

HeartBeat 错误:
FLOW_NO_ROUTE_MATCHED
节点「支付状态判断」没有命中任何出口分支，请检查 true/false/default 连线或条件表达式。
```

常见映射：

| Flowable 场景 | HeartBeat 错误码 | 用户消息 |
| --- | --- | --- |
| exclusiveGateway 无出口命中 | `FLOW_NO_ROUTE_MATCHED` | 条件节点没有命中任何出口分支 |
| serviceTask delegate 抛不可重试异常 | `NODE_EXECUTION_FAILED` | 节点执行失败 |
| Job 重试耗尽 | `NODE_RETRY_EXHAUSTED` | 节点重试次数已耗尽 |
| message correlation 找不到 execution | `FLOW_WAIT_NOT_FOUND` | 没有找到可恢复的等待节点 |
| 多个等待订阅匹配同一事件 | `FLOW_WAIT_AMBIGUOUS` | 外部事件匹配到多个等待节点，需要人工处理 |
| 外部 I/O 命令重试耗尽 | `IO_COMMAND_RETRY_EXHAUSTED` | 外部调用重试次数已耗尽 |
| 外部 I/O 命令超时 | `IO_COMMAND_TIMEOUT` | 外部调用超时 |
| 外部 I/O 状态不明 | `IO_COMMAND_AMBIGUOUS` | 外部调用可能已经执行，需人工确认 |
| 早到事件已过期 | `FLOW_WAIT_EVENT_EXPIRED` | 外部事件已超过等待窗口 |
| task 不存在或已完成 | `FLOW_TASK_NOT_AVAILABLE` | 当前任务不存在或已被处理 |
| 变量序列化失败 | `FLOW_VARIABLE_SERIALIZE_FAILED` | 节点输出过大或包含不可序列化内容 |

### 16.5 业务错误分支

业务失败不一定结束流程：

```text
http_1.error -> log_1.in -> manual_handle.in
```

executor 不抛技术异常，而是：

```json
{
  "status": "SUCCESS",
  "selectedPorts": ["error"],
  "output": {
    "httpStatus": 404,
    "errorMessage": "order not found"
  }
}
```

### 16.6 取消流程

API：

```text
POST /api/v1/flows/runs/{runId}/cancel
```

处理：

```java
runtimeService.deleteProcessInstance(processInstanceId, reason);
```

结果：

- `hb_flow_run.status = CANCELED`
- 写 `FLOW_CANCELED`
- 如果存在等待状态，标记为 `CANCELED`

### 16.7 运行中实例版本处理

首期不做自动迁移正在运行的流程实例，但必须提供人工干预能力。

场景：

- V1 流程已经进入审批等待。
- V2 修复了审批人规则或节点配置。
- V1 运行实例不能自动切换到 V2，否则可能破坏 Flowable 执行树和历史一致性。

提供两个管理员能力：

#### 16.7.1 取消并按新版本重开

API：

```text
POST /api/v1/flows/runs/{runId}/restart-on-active-version
```

处理：

1. 校验 `flow:run:admin` 权限。
2. 读取旧 run 的 `businessKey`、`correlationKey`、`inputSummary`、当前变量摘要。
3. 调用 Flowable `deleteProcessInstance` 取消旧实例，reason 写入 `RESTART_ON_NEW_VERSION`。
4. 旧 run 标记为 `CANCELED`。
5. 基于当前 active version 创建新 run。
6. 新 run 写入 `parent_run_id = oldRunId`、`root_run_id` 和 `restartReason`。
7. 写审计日志。

限制：

- 只允许管理员操作。
- 默认只能对 `WAITING`、`FAILED`、`TIMEOUT` 状态操作。
- 不自动复制 secret，只复制 credential reference。
- 不复制已经完成的人工任务审批结果，除非管理员显式选择携带表单快照。

#### 16.7.2 当前版本内人工跳转

API：

```text
POST /api/v1/flows/runs/{runId}/admin-move
```

用途：

- 将卡住的实例从一个等待节点移动到同一 BPMN 版本内的另一个安全节点。

限制：

- 首期只允许同一 processDefinitionId 内跳转。
- 只能跳转到标记为 `adminRecoverable=true` 的节点。
- 每次跳转必须填写原因。
- 必须记录原 activityId、目标 activityId、操作者、时间和变量变更。
- 不提供任意 Flowable execution tree 手工修改能力。

这两个能力是运维后门，不是普通用户功能。

## 17. 并发和幂等

### 17.1 启动幂等

每次启动必须有 `idempotencyKey`。

来源：

| 触发类型 | 幂等键 |
| --- | --- |
| Manual | 请求传入，不传则后端生成一次性键 |
| Webhook | `X-Idempotency-Key` 或请求体摘要 |
| Cron | `triggerCode + scheduledFireTime` |
| RocketMQ | `topic + messageId` |
| Kafka | `topic + partition + offset` |
| DomainEvent | `eventId` |

### 17.2 节点幂等

节点执行事件唯一键建议：

```text
run_id + node_id + execution_id + attempt_no
```

外部副作用节点必须有业务幂等：

- HTTP 节点发送 `Idempotency-Key` header。
- MQ publish 节点使用 outbox event id。
- Redis set 可天然覆盖，但仍记录 attempt。
- MySQL write 节点首期不开放，后续必须要求幂等键或唯一约束。

### 17.3 普通用户复原重试

管理员后门解决运维干预，不解决普通业务用户“修正入参后重跑”的体验。普通用户需要独立的复原重试 API：

```text
POST /api/v1/flows/runs/{runId}/retry
```

请求：

```json
{
  "idempotencyKey": "retry-order-10001-2",
  "targetVersionMode": "SAME_VERSION",
  "payloadPatch": {
    "amount": 120
  },
  "reason": "修正订单金额后重试"
}
```

处理：

1. 校验当前用户对旧 run 有查看和重试权限，例如 `flow:run:retry` 或 run owner。
2. 只允许从 `FAILED`、`TIMEOUT`、业务允许的 `CANCELED` 状态发起。
3. 读取旧 run 的初始 payload 摘要和 `payloadRef`。
4. 应用 `payloadPatch`，重新执行输入 JSON Schema 校验和 secret 脱敏校验。
5. 使用新的 `idempotency_scope = USER_RETRY` 和新的 `idempotencyKey` 创建新 run。
6. 新 run 写入 `parent_run_id = oldRunId`、`retry_from_run_id = oldRunId`、`root_run_id`、`retry_no = old.retry_no + 1`。
7. 写 `FLOW_RETRIED` 事件，旧 run 不被修改为成功或取消。
8. 启动目标版本流程。

限制：

- 不允许复用原始启动的 `idempotencyKey`，否则会返回旧失败实例。
- 如果用户重复提交同一个 retry `idempotencyKey`，返回同一个 retry run。
- `targetVersionMode` 默认 `SAME_VERSION`，保证可审计和可复现。
- `targetVersionMode = ACTIVE_VERSION` 只有在流程开启 `allowUserRetryOnActiveVersion=true` 且新版本输入 schema 兼容时允许。
- 普通用户 retry 不允许执行 `admin-move`，也不允许跳过已经定义的审批节点。
- retry 形成血缘链，前端运行详情必须展示 `rootRunId`、`retryFromRunId`、`retryNo`。

当普通 `/run` 因相同 `idempotencyKey` 命中旧失败 run 时，接口返回已有 run，并附带：

```json
{
  "canRetry": true,
  "retryEndpoint": "/api/v1/flows/runs/2001/retry"
}
```

### 17.4 并发锁

Flowable 自身管理 execution token 并发。

HeartBeat 侧需要锁的地方：

- 发布同一个 flow 时，锁 `flow_id`。
- 同一个触发器调度时，锁 `trigger_id + fire_time`。
- Outbox 发布时，锁待投递 event。

可以先使用数据库乐观锁/唯一键，后续再引入 Redis 分布式锁。

## 18. 租户、安全和审计

### 18.1 租户

启动 Flowable 实例时传入 tenantId：

```java
runtimeService.startProcessInstanceById(
    processDefinitionId,
    businessKey,
    variables,
    tenantIdString
);
```

所有查询必须带 tenantId：

- Flow definition
- Flow version
- Flow trigger
- Flow run
- Flow task
- Flowable task/process query

### 18.2 权限

建议权限码：

```text
flow:studio:list
flow:definition:edit
flow:definition:publish
flow:run:start
flow:run:cancel
flow:run:retry
flow:run:admin
flow:run:history
flow:task:todo
flow:task:complete
flow:webhook:manage
flow:trigger:manage
```

Webhook 入口不使用用户登录态，但必须校验 webhook key、签名、租户绑定和启用状态。

### 18.3 变量脱敏

不得保存明文：

- password
- token
- secret
- accessKey
- refreshToken
- authorization
- cookie

`FlowableVariableCodec` 写变量前做脱敏策略：

- 运行必要变量可保存。
- secret 仅保存引用。
- 运行历史摘要脱敏。
- 前端查询变量时二次脱敏。

### 18.4 审计

必须审计：

- 发布流程。
- 启用/禁用触发器。
- 手动启动流程。
- 取消流程。
- 完成人工任务。
- 修改凭据。
- Webhook 签名失败。

## 19. 前端展示

### 19.1 运行列表

`/api/v1/flows/{id}/runs` 返回：

```json
{
  "id": "2001",
  "runNo": "FR202607040001",
  "status": "RUNNING",
  "triggerType": "WEBHOOK",
  "businessKey": "order-10001",
  "startedAt": "2026-07-04T10:00:00Z",
  "finishedAt": null,
  "elapsedMs": 1200,
  "currentNodeNames": ["等待支付回调"]
}
```

### 19.2 运行详情

运行详情需要：

- Flow DSL 快照。
- 当前活动节点。
- 已完成节点。
- 失败节点。
- 等待节点。
- 每个节点输入摘要。
- 每个节点输出摘要。
- 错误信息。
- 重试次数。

### 19.3 节点状态颜色

| 状态 | 颜色建议 |
| --- | --- |
| 未执行 | 灰色 |
| 运行中 | 蓝色 |
| 等待中 | 黄色 |
| 成功 | 绿色 |
| 失败 | 红色 |
| 已跳过 | 灰虚线 |
| 已取消 | 深灰 |

### 19.4 运行轨迹回放

Open Flow Studio 需要提供运行轨迹回放，不要求运维人员理解 Flowable execution tree。

回放数据来源：

```text
GET /api/v1/flows/runs/{runId}/replay
```

返回内容：

```json
{
  "runId": "2001",
  "projectionStatus": "PROJECTION_CONSISTENT",
  "events": [
    {
      "eventSeq": 12,
      "eventType": "NODE_COMPLETED",
      "nodeId": "condition_1",
      "sourceNodeId": "condition_1",
      "targetNodeId": "http_1",
      "edgeId": "edge_condition_1_true_http_1",
      "tokenId": "exec-100",
      "selectedPorts": ["true"],
      "inputSummary": {},
      "outputSummary": {},
      "elapsedMs": 35
    }
  ]
}
```

前端表现：

- 按 `event_seq` 在画布上回放真实 token 流转。
- 节点执行时高亮节点，连线命中时高亮 `edge_id`。
- 并行分支使用不同 token 颜色，但默认不展示 Flowable executionId。
- 点击节点可以查看输入摘要、输出摘要、错误摘要和耗时。
- 高级排障模式才展示 `engineInstanceId`、`executionId`、`activityId` 和 BPMN elementId。
- 当 `projectionStatus != PROJECTION_CONSISTENT` 时，页面提示“运行投影同步中”，不把缺失事件误判为流程未执行。

回放约束：

- 前端只消费 HeartBeat 的 nodeId、edgeId、eventSeq，不直接查询 Flowable 表。
- 后端必须保证同一个 run 内 `event_seq` 单调递增。
- `hb_flow_run_event` 缺失 edgeId 时，后端必须通过 `hb_flow_engine_mapping` 和 DSL 快照补齐，补不齐则标记为 `UNKNOWN_EDGE`。
- Replay 是排障和审计能力，不参与流程推进。

## 20. 与现有代码的具体改造点

### 20.1 保留

保留：

- `FlowDefinition`
- `FlowNode`
- `FlowEdge`
- `NodeComponentManifest`
- `NodeComponentRegistryService`
- `ConnectionCredentialService`
- `FlowController`
- `FlowDslValidator`
- `FlowExecutor.debug()` 作为本地调试或单元测试执行器

### 20.2 新增

新增：

```text
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowBpmnCompiler.java
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowBpmnCompileResult.java
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowRuntimeFacade.java
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowTriggerCommand.java
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowResumeCommand.java
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowExpressionSandbox.java
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowRunRetryService.java
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowRunReplayService.java
heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowProjectionPublisher.java

heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/FlowableDeploymentService.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/FlowableRuntimeService.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/FlowableNodeDelegate.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/FlowableTaskBridge.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/FlowableHistoryProjectionService.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/FlowExternalIoCommandDispatcher.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/FlowExternalIoReconcileJob.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/FlowWaitRegistrationListener.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/FlowConditionEvaluator.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/LocalTransactionProjectionPublisher.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/OutboxProjectionPublisher.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/flowable/MqProjectionPublisher.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/buffer/FlowQueueBuffer.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/buffer/MysqlDirectFlowQueueBuffer.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/buffer/RedisStreamFlowQueueBuffer.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/buffer/MqFlowQueueBuffer.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/buffer/FlowRunEventBatchWriter.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/buffer/FlowIoHotStateIndex.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/buffer/FlowWaitHotIndex.java
heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/buffer/FlowBackpressureGuard.java

heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/flow/FlowRunController.java
heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/flow/FlowWebhookController.java
heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/flow/FlowTaskController.java
```

### 20.3 调整

调整：

- `FlowApplicationService.publish`：从“保存版本”升级为“编译 BPMN + Flowable 部署 + 保存部署信息”。
- `FlowApplicationService.debug`：继续走本地 debug，不影响生产。
- `FlowRunRepository`：支持按 `engineInstanceId`、`idempotencyScope + idempotencyKey`、`retryFromRunId`、`rootRunId` 查询。
- `FlowRunStructMapper`：修复当前 UUID runId 与 Long 主键不匹配的问题。生产态 run id 使用 Long，runNo 用字符串。
- `ReliableWorkflowEventService`：从 workflow 专用改名或扩展为 `ReliableEventService`，供 Flow trigger/resume 使用。
- `FlowRunController`：增加 `/runs/{runId}/replay`，返回画布回放事件。

## 21. 分阶段落地

### Phase 0：引擎引入和只读验证

目标：

- 加 Flowable 依赖。
- 用 Flyway 创建 Flowable 表。
- Spring Context 能启动。
- 不接管现有 Flow 执行。

验收：

- 应用启动成功。
- Flowable `RepositoryService`、`RuntimeService`、`TaskService` 可注入。
- 生产环境配置中 `flowable.database-schema-update=false`。

### Phase 1：DSL 编译 BPMN

支持节点：

- manual trigger
- serviceTask
- condition
- log
- end

验收：

- Flow DSL 可编译为合法 BPMN XML。
- 条件表达式必须通过 AST 类型检查和沙盒样本验证。
- 编译结果可部署到 Flowable。
- 映射表可从 BPMN activityId 找回 Flow nodeId。

### Phase 2：手动启动和自动节点执行

支持：

- `POST /api/v1/flows/{id}/run`
- `FlowableNodeDelegate`
- `FlowableProjectionEventListener`
- `FlowableExceptionTranslator`
- `transform.mapper`
- `logic.condition`
- `system.log`
- `system.end`

验收：

- 手动启动流程。
- Flowable 自动推进到结束。
- `hb_flow_run` 状态从 `CREATED` -> `RUNNING` -> `SUCCESS`。
- 前端能看到节点轨迹。
- 投影事件与 Flowable 历史活动一致。
- Flowable 原始异常能翻译为 Flow 节点错误。

### Phase 3：Webhook、Cron、MQ 触发

支持：

- Webhook Controller。
- Quartz 调度触发 Flow。
- RocketMQ/Kafka 触发 Flow。
- Inbox 幂等。

验收：

- 同一 webhook 幂等键重复请求只启动一次。
- 同一 MQ message 重复投递只启动一次。
- Cron 同一 fire time 只启动一次。

### Phase 4：UserTask 和审批迁移

支持：

- `approval.userTask`
- Flowable task 查询。
- Flowable task complete/reject。
- 旧 workflow 待办兼容投影。

验收：

- 流程进入 userTask 后状态为 `WAITING`。
- 用户完成任务后流程继续推进。
- 审批完成写 Outbox 事件。

### Phase 5：等待外部事件和超时

支持：

- `wait.message`
- `wait.timer`
- `timeout` 端口。
- `FlowResumeService`。
- `hb_flow_wait_subscription`。
- Inbox `EARLY_ARRIVED`。

验收：

- 外部事件可恢复等待中的流程。
- 早于 wait.message 提交的外部事件不会丢失，等待订阅提交后能自动恢复。
- 循环或重试再次进入同一等待节点时，旧回调不能被新等待节点误消费。
- Inbox 早到事件过期只软更新为 `EXPIRED`，不在热路径物理删除。
- 重复外部事件不会重复恢复。
- 超时后走 timeout 分支。

### Phase 6：外部 I/O 工作模式

支持：

- `hb_flow_io_command`。
- `FlowExternalIoCommandDispatcher`。
- `FlowExternalIoReconcileJob`。
- HTTP/MySQL/Redis/MQ worker topic 隔离。
- I/O retry、timeout、cancel。

验收：

- HTTP/MySQL/Redis/MQ 节点默认编译为 `EXTERNAL_IO`。
- Flowable delegate 只创建命令，不执行远程调用。
- 下游接口变慢时，Flowable timer、等待恢复和其他流程推进不被 I/O worker 阻塞。
- worker 在 `CALL_STARTED` 后崩溃时，幂等下游必须复用同一 `idempotency_key` 重试。
- 非幂等且不可查询状态的下游必须进入 `FAILED_AMBIGUOUS`，不得自动重试。
- `FAILED_AMBIGUOUS` 优先进入自动对账巡检，超过平台上限才转人工处理。
- I/O 成功、失败、超时都通过 Inbox 恢复等待节点。
- worker 重复投递完成事件不会重复恢复流程。

### Phase 7：运行治理

支持：

- 取消运行。
- 按当前 active version 取消并重开。
- 当前版本内管理员安全跳转。
- 普通用户复原重试。
- 重试策略配置。
- Deadletter 查询。
- 历史归档。
- 运行告警。

验收：

- 可取消运行。
- 可对旧版本等待/失败实例执行管理员重开。
- 可对标记为 recoverable 的节点执行管理员跳转。
- 普通用户可对失败实例修改入参并创建带血缘关系的新 run。
- 失败节点可定位。
- 可查看重试次数和最终错误。

### Phase 8：高频队列中间件和排障增强

支持：

- `FlowQueueBuffer` 抽象。
- `MQ_BUFFER` 作为高频生产推荐模式。
- `MYSQL_DIRECT`、`REDIS_STREAM_BUFFER` 作为低吞吐和短时削峰模式。
- `FlowRunEventBatchWriter`。
- `FlowIoHotStateIndex`。
- `FlowWaitHotIndex`。
- `FlowBackpressureGuard`。
- `FlowProjectionPublisher` 抽象。
- `LOCAL_TX`、`ASYNC_OUTBOX`、`MQ_PROJECTION` 三种投影模式。
- `/api/v1/flows/runs/{runId}/replay`。
- Open Flow Studio 运行轨迹回放。

验收：

- 高频入口可以通过 Kafka/RocketMQ 承载事件流，MySQL 不承担主队列扫描压力。
- MQ consumer ack 必须发生在 MySQL 幂等状态落库成功之后。
- Redis/MQ 中间件故障时，不会破坏 MySQL 中的最终状态账本。
- Redis Stream drainer 重复投递时，MySQL Inbox 幂等仍然生效。
- `hb_flow_run_event` 可按批量 500 条或 100 ms 刷盘，并通过 `event_seq` 幂等去重。
- Redis ZSET/Hash 可作为 I/O 命令热调度索引，但 `CALL_STARTED` 等关键状态必须先 durable。
- Caffeine/Redis wait index 可以减少等待订阅 DB 查询，但不能绕过 Inbox 幂等。
- 超过 TPS、积压或 DB 等待阈值时，入口必须 backpressure。
- 切换到异步投影模式后，投影消费者可以幂等更新 `hb_flow_run_event`。
- `FlowProjectionReconcileJob` 可以修复异步投影延迟或丢失造成的展示缺口。
- 前端 Replay 能按 `event_seq` 高亮节点和边，并隐藏默认 BPMN 细节。

## 22. 测试清单

### 22.1 编译器测试

新增：

```text
heartbeat-application/src/test/java/top/kx/heartbeat/application/flow/runtime/FlowBpmnCompilerTest.java
```

覆盖：

- manual -> startEvent
- end -> endEvent
- service node -> serviceTask
- condition -> exclusiveGateway
- edge -> sequenceFlow
- userTask -> userTask
- wait.message -> intermediateCatchEvent
- invalid port 拒绝发布
- missing end 拒绝发布
- secret in config 拒绝发布
- 用户表达式不会直接编译为业务 UEL。
- 条件 sequenceFlow 使用 `FlowConditionEvaluator`。
- 长耗时 I/O 节点编译为 `EXTERNAL_IO` 形态。

### 22.2 Flowable 集成测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowableRuntimeIntegrationTest.java
```

覆盖：

- 部署 BPMN。
- 启动流程实例。
- serviceTask delegate 被调用。
- 网关正确分支。
- 流程结束。
- 历史可查询。
- Flowable event listener 写入 `hb_flow_run_event`。
- listener 写入失败时 Flowable 推进回滚。
- 补偿任务可从 `ACT_HI_*` 重建缺失投影。
- Flowable Async Executor 不执行长耗时 HTTP/MySQL/Redis/MQ 调用。
- `EXTERNAL_IO` 节点进入等待后由 Inbox 恢复。

### 22.3 表达式沙盒测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowExpressionSandboxTest.java
```

覆盖：

- `payload.amount > 100` 的边界样本。
- `payload.type in ['A','B']` 的数组和枚举样本。
- 缺失字段和 `null` 不抛 Flowable UEL 原始异常。
- AST 执行结果与 `FlowConditionEvaluator` 结果一致。
- 无 default edge 且所有出口无法命中时拒绝发布。

### 22.4 变量和 Payload 测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowVariablePayloadPolicyTest.java
```

覆盖：

- 小 payload 写入受控 Flowable 变量。
- 大 payload 写入 `FlowablePayloadStore`。
- Flowable 变量只保存 `payloadRef`。
- secret 字段不进入 `ACT_RU_VARIABLE`、`ACT_HI_VARINST`、`hb_flow_run_event`。
- 大查询结果不会写入 `ACT_GE_BYTEARRAY` 作为业务数据。

### 22.5 触发器测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowTriggerIdempotencyTest.java
```

覆盖：

- manual idempotency。
- webhook idempotency。
- cron fire time idempotency。
- MQ event idempotency。

### 22.6 人工任务测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowUserTaskIntegrationTest.java
```

覆盖：

- 生成 userTask。
- 待办查询按 assignee 过滤。
- complete 后流程继续。
- reject 走拒绝分支。

### 22.7 等待恢复测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowWaitResumeIntegrationTest.java
```

覆盖：

- 流程进入 wait.message。
- 外部事件恢复。
- 外部事件早于 wait.message 提交时写入 `EARLY_ARRIVED`，等待订阅提交后自动恢复。
- 第二次进入同一 `wait.message` 时生成新的 `waitInstanceId`，不会消费第一次的延迟回调。
- 仅业务主键相同但 `waitInstanceId` 不同的回调不能互相匹配。
- 重复事件不重复恢复。
- 超时走 timeout 分支。

### 22.8 Inbox 并发和 TTL 测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowInboxConcurrencyTest.java
```

覆盖：

- 多线程同时认领同一 `EARLY_ARRIVED` 事件时，只有一个线程条件更新成功。
- `affected_rows = 0` 的线程不会抛系统异常，而是重新读取最终状态。
- 批量扫描使用 `SKIP LOCKED` 时不会阻塞已被其他事务锁定的行。
- TTL 任务只执行 `EARLY_ARRIVED -> EXPIRED` 软过期。
- 消费和 TTL 同时竞争同一行时，只能有一个状态更新成功。
- `EXPIRED` 且未过归档窗口的数据不会被物理删除。

### 22.9 外部 I/O 工作模式测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowExternalIoWorkerTest.java
```

覆盖：

- HTTP/MySQL/Redis/MQ 节点创建 `hb_flow_io_command`。
- delegate 不直接执行远程调用。
- worker 成功后写 Inbox 并恢复流程。
- worker 可重试失败更新 `next_attempt_at`。
- worker 最终失败走 `error` 端口。
- worker 超时走 `timeout` 端口。
- worker 发出请求前必须先写 `CALL_STARTED`。
- 幂等下游重试时复用同一个 `idempotency_key` 和 header。
- `CALL_STARTED + MANUAL_ONLY` 租约过期后进入 `FAILED_AMBIGUOUS`，不自动重试。
- `CALL_STARTED + QUERY_THEN_RETRY` 租约过期后进入 `RECONCILING`。
- I/O worker 池耗尽不阻塞 Flowable timer 和其他流程推进。

### 22.10 队列缓冲和对账测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowQueueBufferAndReconcileTest.java
```

覆盖：

- `MYSQL_DIRECT` 模式直接写 MySQL 状态账本。
- `MQ_BUFFER` 模式通过 Kafka/RocketMQ consumer group 消费事件并幂等落 MySQL。
- MQ consumer 在 MySQL 幂等落库成功后才 ack。
- MQ 重复投递或 rebalance 后不会重复启动 run 或重复恢复等待节点。
- `REDIS_STREAM_BUFFER` 重复投递后 MySQL Inbox 幂等仍然生效。
- Redis Stream 只作为短时削峰，不能绕过 MySQL 状态账本。
- Redis 不可用时降级到 `MYSQL_DIRECT` 或入口限流，不静默丢失事件。
- `FlowRunEventBatchWriter` 达到 500 条或 100 ms 时批量 multi-values insert。
- 重复 `event_seq` 使用 `INSERT IGNORE` 或等价幂等写入不报错。
- Redis ZSET 抢 I/O 命令时，不访问 MySQL 扫描待处理队列。
- Redis 丢失后可以从 MySQL 未完成 command 重建 ready ZSET。
- `CALL_STARTED` 未 durable 前不得执行外部副作用。
- Caffeine/Redis wait index 命中时减少 DB 查询，但恢复仍经过 Inbox 幂等。
- 触发反压阈值时，Webhook 返回 429 或 MQ consumer 暂停拉取。
- 分区/分表 repository 能按 runId/tenantId/month 定位物理表。
- `FAILED_AMBIGUOUS` 先进入 `RECONCILING`，不立即创建人工工单。
- 对账探针查到下游成功后，命令转 `SUCCEEDED` 并写 Inbox 恢复流程。
- 对账探针超过上限后，命令转 `MANUAL_REQUIRED`，工单按连接和错误类型聚合。

### 22.11 投影模式测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowProjectionPublisherTest.java
```

覆盖：

- `LOCAL_TX` 模式投影写入失败时回滚 Flowable 推进事务。
- `ASYNC_OUTBOX` 模式只写投影 outbox，不跨库直写业务表。
- 投影消费者重复消费同一 `projection_event_id` 时幂等。
- `FlowProjectionReconcileJob` 可以从 Flowable 历史补齐缺失投影。
- 前端能识别 `PROJECTION_SYNCING`、`PROJECTION_DELAYED`、`PROJECTION_CONSISTENT`。

### 22.12 运行回放测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowRunReplayTest.java
```

覆盖：

- `/api/v1/flows/runs/{runId}/replay` 按 `event_seq` 返回事件。
- 回放事件包含 nodeId、edgeId、selectedPorts、input/output summary。
- 并行分支保留 `token_id`，前端默认隐藏 Flowable executionId。
- 缺失 edgeId 时后端能基于 DSL 快照和 mapping 补齐，补不齐标记 `UNKNOWN_EDGE`。

### 22.13 异常翻译测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowableExceptionTranslatorTest.java
```

覆盖：

- exclusiveGateway 无出口命中翻译为 `FLOW_NO_ROUTE_MATCHED`。
- Job 重试耗尽翻译为 `NODE_RETRY_EXHAUSTED`。
- message correlation 找不到等待实例翻译为 `FLOW_WAIT_NOT_FOUND`。
- 多等待订阅命中翻译为 `FLOW_WAIT_AMBIGUOUS`。
- 外部 I/O 最终失败翻译为 `IO_COMMAND_RETRY_EXHAUSTED`。
- 外部 I/O 超时翻译为 `IO_COMMAND_TIMEOUT`。
- 外部 I/O 状态不明翻译为 `IO_COMMAND_AMBIGUOUS`。
- 早到事件已过期翻译为 `FLOW_WAIT_EVENT_EXPIRED`。
- Flowable activityId 能映射回 Flow nodeId 和中文节点名。

### 22.14 管理员运维测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowRunAdminOperationTest.java
```

覆盖：

- 等待中的旧版本实例可取消并按 active version 重开。
- 新 run 记录 `parent_run_id`。
- 普通用户不能执行重开和跳转。
- 未标记 `adminRecoverable` 的节点不能跳转。
- 管理员操作写审计日志。

### 22.15 普通用户复原重试测试

新增：

```text
heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowRunUserRetryTest.java
```

覆盖：

- FAILED run 可用新 `idempotencyKey` 创建 retry run。
- retry run 写入 `parent_run_id`、`retry_from_run_id`、`root_run_id`、`retry_no`。
- 重复 retry `idempotencyKey` 返回同一个 retry run。
- 复用原始启动 `idempotencyKey` 不会新建 run。
- 普通用户不能执行管理员重开和节点跳转。

### 22.16 安全测试

覆盖：

- webhook 签名错误拒绝。
- 跨租户无法查询流程运行。
- 节点变量脱敏。
- 凭据 secret 不进入 Flow DSL 和运行历史。

## 23. 验收标准

最终必须满足：

- Flow DSL 发布后能生成 BPMN 并部署到 Flowable。
- 生产态运行不再依赖 `FlowExecutor.debug()` 的内存队列。
- 当前节点执行后，由 Flowable sequenceFlow/gateway 推进到下一个节点。
- 开始节点、结束节点、服务节点、条件节点、人工任务节点、等待节点都有明确语义。
- `hb_flow_run` 和 `hb_flow_run_event` 通过 Flowable 全局事件监听器投影，投影失败必须回滚 Flowable 推进事务。
- 补偿任务能从 Flowable 历史表重建缺失投影，且不得反向修改 Flowable 状态。
- Flowable 变量执行瘦 Payload 策略，大 payload 只在变量中保留 `payloadRef`。
- 长耗时 I/O 节点默认使用外部工作者/命令表模式，不占用 Flowable Async Executor 等待远程调用。
- Flowable Async Executor 只承载短任务、timer 和引擎 Job，不因下游接口变慢导致全局流程阻塞。
- I/O worker 在 `CALL_STARTED` 后崩溃时，支持幂等的下游必须复用同一外部幂等键重试；不支持幂等且不可查询状态的下游必须进入 `FAILED_AMBIGUOUS`。
- 所有有副作用的外部调用必须把 `hb_flow_io_command.idempotency_key` 穿透给下游，或在编译期被限制为 `MANUAL_ONLY`。
- `FAILED_AMBIGUOUS` 必须优先由自动对账探针处理，超过平台上限后才转人工介入。
- 外部回调早于等待节点持久化时进入 Inbox `EARLY_ARRIVED`，等待订阅提交后可以自动消费恢复。
- 等待恢复必须使用执行级 `waitInstanceId` 或等价 correlation，循环和重试场景不得复用上一轮业务主键 correlation。
- Inbox 早到事件认领必须使用条件更新或 `SKIP LOCKED` 队列式扫描，不得在高频路径使用阻塞式 `SELECT ... FOR UPDATE`。
- Inbox TTL 清理只能软更新为 `EXPIRED`，热路径禁止物理删除，归档窗口后才能搬迁或删除。
- MySQL 是最终状态账本；Redis/MQ 缓冲层只能用于削峰、通知或可靠事件流，不得让流程状态只存在缓冲层。
- 高频生产场景必须支持 Kafka/RocketMQ 等中间件承载消息流转，MySQL 不作为主高吞吐队列使用。
- MQ consumer 必须在 MySQL 幂等状态落库成功后 ack，重复投递依赖 MySQL 幂等账本去重。
- 极致高并发模式下，`hb_flow_run_event` 必须支持批量刷盘，禁止单条循环插入作为默认实现。
- Redis 热状态只能作为调度索引和 wait index，加速读写；`CALL_STARTED` 等关键状态必须先写入 durable ledger。
- `hb_flow_run_event` 必须具备分区/分表或归档方案，`hb_flow_io_command` 必须具备租户哈希分片预案。
- 未启用 `MQ_BUFFER` 的生产环境，单租户 TPS 超过 50 必须限流。
- 代码必须通过 `FlowProjectionPublisher` 写投影，支持 `LOCAL_TX` 和未来 `ASYNC_OUTBOX` / `MQ_PROJECTION` 切换。
- 条件表达式由 HeartBeat AST 和 `FlowConditionEvaluator` 执行，并在编译期通过 mock payload 沙盒验证。
- Flowable 原始异常必须翻译为 Flow 节点级错误，前端不展示 BPMN 内部错误文本。
- Webhook/MQ/Cron/手动触发都统一进入 `FlowTriggerService`。
- 外部事件恢复统一走 Inbox 幂等。
- 节点副作用通过 Outbox 或业务幂等键保护。
- 运行历史能映射回 Open Flow Studio 画布节点。
- Open Flow Studio 必须提供按 `event_seq` 回放真实节点和连线流转的 Replay 能力。
- 多租户查询和 Flowable tenantId 一致。
- 生产环境不允许 Flowable 自动建表。
- 不允许流程 DSL 保存明文 secret。
- 旧 workflow 模块有迁移兼容路径。
- 管理员可以取消旧版本等待/失败实例并按当前 active version 重开。
- 管理员跳转只允许在同一流程定义版本内、且仅限标记为 recoverable 的节点。
- 普通业务用户可以对失败 run 发起带血缘关系的复原重试，且不会被原始启动 `idempotencyKey` 挡住。

## 24. 不做的事

首期不做：

- 用户上传 Java/Groovy/JS 脚本节点。
- Flowable Modeler 替代 Open Flow Studio 画布。
- 直接暴露 Flowable REST API 给前端。
- 使用 Flowable Identity 管理 HeartBeat 用户。
- 多数据源分库部署 Flowable。
- 引入 Temporal Server 或 Zeebe Broker。
- 自动迁移正在运行的流程实例到新版本。首期只提供管理员显式取消重开和同版本安全跳转。

## 25. 自检

- 文档已明确选择 Flowable，而不是同时实现 Temporal/Camunda/Zeebe。
- 文档保持 HeartBeat Flow DSL 作为上层契约，不把画布绑定到 Flowable Modeler。
- 文档明确 Flowable 负责生产态 token 推进、等待、任务、定时器和历史。
- 文档明确 HeartBeat 保留租户、权限、审计、凭据和运行投影。
- 文档给出表结构、服务类、API、编译规则、运行链路、测试和分阶段验收。
- 文档没有把当前 debug executor 直接包装成生产引擎。
- 文档没有要求在生产环境自动创建 Flowable 表。
- 文档明确当前阶段投影一致性由 Flowable 全局事件监听器和同事务回滚保护，并预留异步投影演进。
- 文档明确采用瘦 Payload，避免 Flowable 变量表保存大型业务数据。
- 文档明确长耗时 I/O 节点使用外部工作者/命令表模式，避免阻塞 Flowable/Tomcat 工作线程和 Flowable Async Executor。
- 文档明确 `CALL_STARTED` 后的 worker 崩溃按外部幂等、状态查询或 `FAILED_AMBIGUOUS` 分支处理，不盲目重发非幂等请求。
- 文档明确 `FAILED_AMBIGUOUS` 优先自动对账，避免人工工单洪峰。
- 文档明确高频生产场景应使用 Kafka/RocketMQ 等中间件承载消息流转，MySQL 不承担主高吞吐队列职责。
- 文档明确 MySQL 是最终状态账本，Redis/MQ 负责削峰、消费组调度或可靠事件流。
- 文档明确 `hb_flow_run_event` 支持批量刷盘和分区归档，降低 redo log 和索引写入压力。
- 文档明确 Redis 热状态只作为调度索引，不能承载不可逆关键状态的唯一真相。
- 文档明确入口 backpressure 红线，避免数据库被积压流量压垮。
- 文档明确 Inbox 支持 `EARLY_ARRIVED`，可以处理回调早于等待状态持久化的竞态。
- 文档明确等待 correlation 必须达到节点执行级唯一性，避免循环或重试误消费旧回调。
- 文档明确 Inbox 抢占使用条件更新或 `SKIP LOCKED`，降低行锁热点。
- 文档明确 TTL 只能软过期，物理删除必须在归档窗口之后。
- 文档明确 Open Flow Studio 需要运行轨迹 Replay，降低 Flowable/BPMN 黑盒排障成本。
- 文档明确用户表达式不直接编译为业务 UEL，必须通过 AST、受控 evaluator 和编译期沙盒验证。
- 文档明确 Flowable 原始异常必须翻译为 Flow 节点级业务错误。
- 文档明确不自动迁移运行中实例，但提供管理员显式重开、安全跳转，以及普通用户带血缘关系的复原重试能力。
