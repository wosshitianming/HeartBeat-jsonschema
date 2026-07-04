ALTER TABLE `hb_flow_definition`
    ADD COLUMN `runtime_engine` VARCHAR(32) NOT NULL DEFAULT 'FLOWABLE' COMMENT '生产态运行时引擎' AFTER `active_version_no`,
    ADD COLUMN `active_process_definition_id` VARCHAR(128) NULL COMMENT '当前激活Flowable流程定义ID' AFTER `runtime_engine`,
    ADD COLUMN `active_deployment_id` VARCHAR(128) NULL COMMENT '当前激活Flowable部署ID' AFTER `active_process_definition_id`;

ALTER TABLE `hb_flow_version`
    ADD COLUMN `runtime_engine` VARCHAR(32) NOT NULL DEFAULT 'FLOWABLE' COMMENT '生产态运行时引擎' AFTER `compile_report`,
    ADD COLUMN `bpmn_xml` MEDIUMTEXT NULL COMMENT '生产态BPMN XML' AFTER `runtime_engine`,
    ADD COLUMN `bpmn_sha256` CHAR(64) NULL COMMENT 'BPMN内容摘要' AFTER `bpmn_xml`,
    ADD COLUMN `deployment_id` VARCHAR(128) NULL COMMENT 'Flowable部署ID' AFTER `bpmn_sha256`,
    ADD COLUMN `process_definition_id` VARCHAR(128) NULL COMMENT 'Flowable流程定义ID' AFTER `deployment_id`,
    ADD COLUMN `process_definition_key` VARCHAR(128) NULL COMMENT 'Flowable流程定义KEY' AFTER `process_definition_id`,
    ADD COLUMN `compile_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '编译状态' AFTER `process_definition_key`,
    ADD COLUMN `compile_error` TEXT NULL COMMENT '编译错误' AFTER `compile_status`,
    ADD COLUMN `deployed_at` DATETIME(3) NULL COMMENT '部署时间' AFTER `compile_error`;

ALTER TABLE `hb_flow_run`
    ADD COLUMN `run_no` VARCHAR(64) NULL COMMENT '运行编号' AFTER `flow_id`,
    ADD COLUMN `engine` VARCHAR(32) NOT NULL DEFAULT 'FLOWABLE' COMMENT '运行时引擎' AFTER `run_no`,
    ADD COLUMN `engine_instance_id` VARCHAR(128) NULL COMMENT '引擎流程实例ID' AFTER `engine`,
    ADD COLUMN `process_definition_id` VARCHAR(128) NULL COMMENT '引擎流程定义ID' AFTER `engine_instance_id`,
    ADD COLUMN `flow_version_id` BIGINT UNSIGNED NULL COMMENT '流程版本ID' AFTER `process_definition_id`,
    ADD COLUMN `trigger_id` BIGINT UNSIGNED NULL COMMENT '触发器ID' AFTER `flow_version_id`,
    ADD COLUMN `trigger_key` VARCHAR(128) NULL COMMENT '触发器KEY' AFTER `trigger_id`,
    ADD COLUMN `idempotency_key` VARCHAR(128) NULL COMMENT '幂等键' AFTER `trigger_key`,
    ADD COLUMN `idempotency_scope` VARCHAR(32) NOT NULL DEFAULT 'START' COMMENT '幂等范围' AFTER `idempotency_key`,
    ADD COLUMN `business_key` VARCHAR(128) NULL COMMENT '业务键' AFTER `idempotency_scope`,
    ADD COLUMN `correlation_key` VARCHAR(128) NULL COMMENT '关联键' AFTER `business_key`,
    ADD COLUMN `parent_run_id` BIGINT UNSIGNED NULL COMMENT '父运行ID' AFTER `correlation_key`,
    ADD COLUMN `root_run_id` BIGINT UNSIGNED NULL COMMENT '根运行ID' AFTER `parent_run_id`,
    ADD COLUMN `retry_from_run_id` BIGINT UNSIGNED NULL COMMENT '复原重试来源运行ID' AFTER `root_run_id`,
    ADD COLUMN `retry_no` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试序号' AFTER `retry_from_run_id`,
    ADD COLUMN `retry_reason` VARCHAR(255) NULL COMMENT '重试原因' AFTER `retry_no`;

CREATE UNIQUE INDEX `uk_flow_run_idempotency`
    ON `hb_flow_run` (`tenant_id`, `idempotency_scope`, `idempotency_key`);

ALTER TABLE `hb_flow_run_event`
    ADD COLUMN `event_seq` BIGINT UNSIGNED NULL COMMENT '运行内事件序号' AFTER `run_id`,
    ADD COLUMN `engine_activity_id` VARCHAR(128) NULL COMMENT '引擎活动ID' AFTER `event_seq`,
    ADD COLUMN `execution_id` VARCHAR(128) NULL COMMENT '引擎执行ID' AFTER `engine_activity_id`,
    ADD COLUMN `task_id` VARCHAR(128) NULL COMMENT '引擎任务ID' AFTER `execution_id`,
    ADD COLUMN `source_node_id` VARCHAR(128) NULL COMMENT '源节点ID' AFTER `node_id`,
    ADD COLUMN `target_node_id` VARCHAR(128) NULL COMMENT '目标节点ID' AFTER `source_node_id`,
    ADD COLUMN `edge_id` VARCHAR(128) NULL COMMENT '连线ID' AFTER `target_node_id`,
    ADD COLUMN `token_id` VARCHAR(128) NULL COMMENT '执行令牌ID' AFTER `edge_id`,
    ADD COLUMN `attempt_no` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '执行尝试次数' AFTER `token_id`,
    ADD COLUMN `selected_ports` JSON NULL COMMENT '命中端口' AFTER `output_json`,
    ADD COLUMN `input_payload_ref` BIGINT UNSIGNED NULL COMMENT '输入载荷引用' AFTER `selected_ports`,
    ADD COLUMN `output_payload_ref` BIGINT UNSIGNED NULL COMMENT '输出载荷引用' AFTER `input_payload_ref`,
    ADD COLUMN `event_summary` JSON NULL COMMENT '事件展示摘要' AFTER `output_payload_ref`,
    ADD COLUMN `error_code` VARCHAR(64) NULL COMMENT '错误编码' AFTER `event_summary`;

CREATE UNIQUE INDEX `uk_flow_run_event_seq`
    ON `hb_flow_run_event` (`tenant_id`, `run_id`, `event_seq`);

CREATE INDEX `idx_flow_run_event_node`
    ON `hb_flow_run_event` (`tenant_id`, `run_id`, `source_node_id`, `event_type`);

CREATE TABLE `hb_flow_trigger`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程触发器ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `flow_id` BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `flow_version_id` BIGINT UNSIGNED NULL COMMENT '流程版本ID',
    `trigger_code` VARCHAR(128) NOT NULL COMMENT '触发器编码',
    `trigger_type` VARCHAR(32) NOT NULL COMMENT '触发器类型',
    `webhook_key` VARCHAR(128) NULL COMMENT 'Webhook键',
    `cron_expression` VARCHAR(128) NULL COMMENT 'Cron表达式',
    `event_topic` VARCHAR(128) NULL COMMENT '领域事件主题',
    `mq_type` VARCHAR(32) NULL COMMENT '消息队列类型',
    `mq_topic` VARCHAR(255) NULL COMMENT '消息主题',
    `mq_tag` VARCHAR(255) NULL COMMENT '消息标签',
    `config_json` JSON NULL COMMENT '触发器配置',
    `status` VARCHAR(32) NOT NULL COMMENT '触发器状态',
    `last_triggered_at` DATETIME(3) NULL COMMENT '最后触发时间',
    `created_by` BIGINT UNSIGNED NULL COMMENT '创建人ID',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人ID',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_trigger_code` (`tenant_id`, `trigger_code`),
    UNIQUE KEY `uk_flow_webhook_key` (`tenant_id`, `webhook_key`),
    KEY `idx_flow_trigger_flow` (`tenant_id`, `flow_id`),
    KEY `idx_flow_trigger_type_status` (`tenant_id`, `trigger_type`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程触发器';

CREATE TABLE `hb_flow_engine_mapping`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '映射ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `flow_id` BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `flow_version_id` BIGINT UNSIGNED NOT NULL COMMENT '流程版本ID',
    `flow_node_id` VARCHAR(128) NOT NULL COMMENT 'Flow节点ID',
    `bpmn_element_id` VARCHAR(128) NOT NULL COMMENT 'BPMN元素ID',
    `component_type` VARCHAR(128) NOT NULL COMMENT '组件类型',
    `component_version` VARCHAR(32) NOT NULL COMMENT '组件版本',
    `executor_id` VARCHAR(128) NOT NULL COMMENT '执行器ID',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_node_mapping` (`tenant_id`, `flow_version_id`, `flow_node_id`),
    KEY `idx_flow_bpmn_mapping` (`tenant_id`, `flow_version_id`, `bpmn_element_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Flow与BPMN元素映射';

CREATE TABLE `hb_flow_wait_subscription`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '等待订阅ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id` BIGINT UNSIGNED NOT NULL COMMENT '运行ID',
    `engine_instance_id` VARCHAR(128) NOT NULL COMMENT '引擎流程实例ID',
    `execution_id` VARCHAR(128) NOT NULL COMMENT '引擎执行ID',
    `node_id` VARCHAR(128) NOT NULL COMMENT '等待节点ID',
    `wait_instance_id` VARCHAR(128) NOT NULL COMMENT '等待实例ID',
    `message_name` VARCHAR(128) NULL COMMENT '消息名称',
    `correlation_key` VARCHAR(128) NULL COMMENT '关联键',
    `status` VARCHAR(32) NOT NULL COMMENT '等待状态',
    `expire_at` DATETIME(3) NULL COMMENT '过期时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_wait_instance` (`tenant_id`, `wait_instance_id`),
    KEY `idx_flow_wait_correlation` (`tenant_id`, `message_name`, `correlation_key`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程等待订阅';

CREATE TABLE `hb_flow_io_command`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '外部IO命令ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id` BIGINT UNSIGNED NOT NULL COMMENT '运行ID',
    `node_id` VARCHAR(128) NOT NULL COMMENT '节点ID',
    `command_type` VARCHAR(64) NOT NULL COMMENT '命令类型',
    `idempotency_key` VARCHAR(128) NOT NULL COMMENT '外部幂等键',
    `request_json` JSON NULL COMMENT '请求摘要',
    `response_json` JSON NULL COMMENT '响应摘要',
    `status` VARCHAR(32) NOT NULL COMMENT '命令状态',
    `attempt_no` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '尝试次数',
    `next_attempt_at` DATETIME(3) NULL COMMENT '下次尝试时间',
    `lease_owner` VARCHAR(128) NULL COMMENT '租约持有人',
    `lease_until` DATETIME(3) NULL COMMENT '租约截止时间',
    `error_code` VARCHAR(64) NULL COMMENT '错误编码',
    `error_message` TEXT NULL COMMENT '错误信息',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_io_idempotency` (`tenant_id`, `idempotency_key`),
    KEY `idx_flow_io_ready` (`tenant_id`, `status`, `next_attempt_at`),
    KEY `idx_flow_io_run` (`tenant_id`, `run_id`, `node_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程外部IO命令';

CREATE TABLE `hb_flow_payload`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Payload ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id` BIGINT UNSIGNED NULL COMMENT '运行ID',
    `payload_sha256` CHAR(64) NOT NULL COMMENT 'Payload摘要',
    `payload_json` JSON NOT NULL COMMENT '脱敏后的Payload内容',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_flow_payload_run` (`tenant_id`, `run_id`),
    KEY `idx_flow_payload_hash` (`tenant_id`, `payload_sha256`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程Payload瘦身存储';
