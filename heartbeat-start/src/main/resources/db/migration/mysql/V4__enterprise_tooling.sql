CREATE TABLE `sys_gen_table`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '生成表配置ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `table_name`    VARCHAR(128)    NOT NULL COMMENT '数据库表名',
    `table_comment` VARCHAR(512)    NULL COMMENT '数据库表说明',
    `class_name`    VARCHAR(128)    NOT NULL COMMENT '生成类名',
    `module_name`   VARCHAR(64)     NOT NULL COMMENT '业务模块名',
    `base_package`  VARCHAR(256)    NOT NULL COMMENT '基础包名',
    `resource_key`  VARCHAR(128)    NOT NULL COMMENT '前端资源标识',
    `options_json`  JSON            NULL COMMENT '扩展生成选项',
    `status`        VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`       INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gen_table_name` (`tenant_id`, `table_name`, `delete_marker`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='代码生成表配置';

CREATE TABLE `sys_gen_column`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '生成字段ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `gen_table_id`   BIGINT UNSIGNED NOT NULL COMMENT '生成表配置ID',
    `column_name`    VARCHAR(128)    NOT NULL COMMENT '数据库字段名',
    `column_comment` VARCHAR(512)    NULL COMMENT '字段说明',
    `data_type`      VARCHAR(128)    NOT NULL COMMENT '数据库类型',
    `java_type`      VARCHAR(64)     NOT NULL COMMENT 'Java类型',
    `java_field`     VARCHAR(128)    NOT NULL COMMENT 'Java属性名',
    `primary_key`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否主键',
    `auto_increment` TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否自增',
    `nullable`       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否可空',
    `sort_no`        INT             NOT NULL DEFAULT 0 COMMENT '字段顺序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gen_column_name` (`tenant_id`, `gen_table_id`, `column_name`),
    KEY `idx_gen_column_table` (`tenant_id`, `gen_table_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='代码生成字段配置';

CREATE TABLE `sys_job`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `job_code`        VARCHAR(96)     NOT NULL COMMENT '任务编码',
    `job_name`        VARCHAR(128)    NOT NULL COMMENT '任务名称',
    `job_group`       VARCHAR(96)     NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组',
    `invoke_target`   VARCHAR(256)    NOT NULL COMMENT '调用目标',
    `cron_expression` VARCHAR(128)    NOT NULL COMMENT 'Cron表达式',
    `misfire_policy`  VARCHAR(32)     NOT NULL DEFAULT 'SMART' COMMENT '错失策略',
    `concurrent`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否允许并发',
    `status`          VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_code` (`tenant_id`, `job_code`, `delete_marker`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='定时任务';

CREATE TABLE `sys_job_log`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `job_id`        BIGINT UNSIGNED NULL COMMENT '任务ID',
    `job_code`      VARCHAR(96)     NOT NULL COMMENT '任务编码',
    `invoke_target` VARCHAR(256)    NOT NULL COMMENT '调用目标',
    `result_status` VARCHAR(16)     NOT NULL COMMENT '执行结果',
    `message`       VARCHAR(1000)   NULL COMMENT '执行消息',
    `duration_ms`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '耗时毫秒',
    `started_at`    DATETIME(3)     NOT NULL COMMENT '开始时间',
    `finished_at`   DATETIME(3)     NOT NULL COMMENT '结束时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_job_log_job_time` (`tenant_id`, `job_id`, `started_at`),
    KEY `idx_job_log_status_time` (`tenant_id`, `result_status`, `started_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='定时任务执行日志';
