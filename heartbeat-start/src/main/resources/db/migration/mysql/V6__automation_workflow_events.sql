CREATE TABLE `hb_node_component`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '节点组件ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `type`          VARCHAR(128)    NOT NULL COMMENT '组件类型',
    `version`       VARCHAR(32)     NOT NULL COMMENT '组件版本',
    `name`          VARCHAR(128)    NOT NULL COMMENT '组件名称',
    `category`      VARCHAR(64)     NOT NULL COMMENT '组件分类',
    `source`        VARCHAR(32)     NOT NULL COMMENT '组件来源',
    `manifest_json` JSON            NOT NULL COMMENT '组件清单',
    `status`        VARCHAR(16)     NOT NULL COMMENT '状态',
    `sort_no`       INT             NOT NULL COMMENT '排序号',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hb_node_component_type_version` (`tenant_id`, `type`, `version`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='节点组件';

CREATE TABLE `hb_flow_definition`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
    `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`              VARCHAR(128)    NOT NULL COMMENT '流程名称',
    `code`              VARCHAR(128)    NOT NULL COMMENT '流程编码',
    `description`       VARCHAR(512)    NULL COMMENT '流程说明',
    `status`            VARCHAR(16)     NOT NULL COMMENT '状态',
    `active_version_no` INT             NULL COMMENT '当前启用版本号',
    `dsl_json`          JSON            NULL COMMENT '流程DSL',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hb_flow_definition_code` (`tenant_id`, `code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程定义';

CREATE TABLE `hb_flow_version`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程版本ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `flow_id`        BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `version_no`     INT             NOT NULL COMMENT '版本号',
    `dsl_json`       JSON            NOT NULL COMMENT '流程DSL',
    `compile_report` JSON            NULL COMMENT '编译报告',
    `status`         VARCHAR(16)     NOT NULL COMMENT '状态',
    `published_by`   BIGINT UNSIGNED NULL COMMENT '发布人',
    `published_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发布时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hb_flow_version_no` (`tenant_id`, `flow_id`, `version_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程版本';

CREATE TABLE `hb_connection_credential`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '连接凭据ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`        VARCHAR(128)    NOT NULL COMMENT '凭据名称',
    `type`        VARCHAR(32)     NOT NULL COMMENT '凭据类型',
    `config_json` JSON            NOT NULL COMMENT '连接配置',
    `secret_json` JSON            NULL COMMENT '敏感配置',
    `status`      VARCHAR(16)     NOT NULL COMMENT '状态',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='连接凭据';

CREATE TABLE `hb_flow_run`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程运行ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `flow_id`        BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `version_no`     INT             NOT NULL COMMENT '版本号',
    `trigger_type`   VARCHAR(64)     NOT NULL COMMENT '触发类型',
    `status`         VARCHAR(16)     NOT NULL COMMENT '运行状态',
    `input_summary`  JSON            NULL COMMENT '输入摘要',
    `output_summary` JSON            NULL COMMENT '输出摘要',
    `error_message`  TEXT            NULL COMMENT '错误信息',
    `started_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
    `finished_at`    DATETIME(3)     NULL COMMENT '结束时间',
    `elapsed_ms`     BIGINT          NULL COMMENT '耗时毫秒',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_hb_flow_run_status` (`tenant_id`, `flow_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程运行记录';

CREATE TABLE `hb_flow_run_event`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程运行事件ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id`        BIGINT UNSIGNED NOT NULL COMMENT '流程运行ID',
    `node_id`       VARCHAR(128)    NOT NULL COMMENT '节点ID',
    `node_type`     VARCHAR(128)    NOT NULL COMMENT '节点类型',
    `event_type`    VARCHAR(32)     NOT NULL COMMENT '事件类型',
    `input_json`    JSON            NULL COMMENT '输入数据',
    `output_json`   JSON            NULL COMMENT '输出数据',
    `error_message` TEXT            NULL COMMENT '错误信息',
    `elapsed_ms`    BIGINT          NULL COMMENT '耗时毫秒',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_hb_flow_run_event_run` (`tenant_id`, `run_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程运行事件';

CREATE TABLE `flow_wait_state`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程等待状态ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id`          BIGINT UNSIGNED NOT NULL COMMENT '流程运行ID',
    `node_id`         VARCHAR(128)    NOT NULL COMMENT '节点ID',
    `correlation_key` VARCHAR(128)    NOT NULL COMMENT '关联键',
    `status`          VARCHAR(16)     NOT NULL COMMENT '等待状态',
    `payload_json`    JSON            NULL COMMENT '等待载荷',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_wait_state_correlation` (`tenant_id`, `correlation_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程等待状态';

CREATE TABLE `wf_process_definition`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审批流程定义ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`           VARCHAR(128)    NOT NULL COMMENT '流程名称',
    `definition_key` VARCHAR(128)    NOT NULL COMMENT '流程定义键',
    `version_no`     INT             NOT NULL COMMENT '版本号',
    `form_schema`    JSON            NULL COMMENT '表单Schema',
    `status`         VARCHAR(16)     NOT NULL COMMENT '状态',
    `deployed_at`    DATETIME(3)     NULL COMMENT '部署时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wf_process_definition_key` (`tenant_id`, `definition_key`, `version_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='审批流程定义';

CREATE TABLE `wf_process_instance`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审批流程实例ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `definition_id`   BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `business_key`    VARCHAR(128)    NULL COMMENT '业务键',
    `title`           VARCHAR(128)    NOT NULL COMMENT '实例标题',
    `initiator_id`    BIGINT UNSIGNED NULL COMMENT '发起人ID',
    `status`          VARCHAR(16)     NOT NULL COMMENT '实例状态',
    `current_task_id` BIGINT UNSIGNED NULL COMMENT '当前任务ID',
    `payload`         JSON            NULL COMMENT '业务载荷',
    `started_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
    `ended_at`        DATETIME(3)     NULL COMMENT '结束时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_wf_process_instance_status` (`tenant_id`, `status`, `started_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='审批流程实例';

CREATE TABLE `wf_task`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审批任务ID',
    `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `instance_id`  BIGINT UNSIGNED NOT NULL COMMENT '流程实例ID',
    `name`         VARCHAR(128)    NOT NULL COMMENT '任务名称',
    `assignee_id`  BIGINT UNSIGNED NULL COMMENT '处理人ID',
    `status`       VARCHAR(16)     NOT NULL COMMENT '任务状态',
    `comment`      VARCHAR(512)    NULL COMMENT '任务备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `completed_at` DATETIME(3)     NULL COMMENT '完成时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_wf_task_assignee` (`tenant_id`, `assignee_id`, `status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='审批任务';

CREATE TABLE `wf_task_action`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审批任务操作ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `task_id`     BIGINT UNSIGNED NOT NULL COMMENT '审批任务ID',
    `action`      VARCHAR(16)     NOT NULL COMMENT '操作类型',
    `operator_id` BIGINT UNSIGNED NULL COMMENT '操作人ID',
    `comment`     VARCHAR(512)    NULL COMMENT '操作备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_wf_task_action_task` (`tenant_id`, `task_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='审批任务操作';

CREATE TABLE `sys_outbox_event`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '出站事件ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `event_id`       VARCHAR(64)     NOT NULL COMMENT '事件ID',
    `event_type`     VARCHAR(96)     NOT NULL COMMENT '事件类型',
    `aggregate_type` VARCHAR(64)     NOT NULL COMMENT '聚合类型',
    `aggregate_id`   VARCHAR(64)     NOT NULL COMMENT '聚合ID',
    `payload_json`   JSON            NOT NULL COMMENT '事件载荷',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'NEW' COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `published_at`   DATETIME(3)     NULL COMMENT '发布时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_outbox_event_id` (`event_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='出站事件';

CREATE TABLE `sys_inbox_event`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '入站事件ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `consumer_code` VARCHAR(64)     NOT NULL COMMENT '消费者编码',
    `event_id`      VARCHAR(64)     NOT NULL COMMENT '事件ID',
    `status`        VARCHAR(16)     NOT NULL DEFAULT 'PROCESSED' COMMENT '状态',
    `processed_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '处理时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_inbox_consumer_event` (`consumer_code`, `event_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='入站事件';
