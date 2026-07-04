CREATE TABLE `structure_definition`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '结构定义ID',
    `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`              VARCHAR(128)    NOT NULL COMMENT '结构名称',
    `description`       VARCHAR(512)    NULL COMMENT '结构描述',
    `active_version_no` INT             NULL COMMENT '当前启用版本号',
    `status`            VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_structure_definition_tenant_status` (`tenant_id`, `status`, `update_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='结构定义';

CREATE TABLE `structure_draft`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '结构草稿ID',
    `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `definition_id`     BIGINT UNSIGNED NOT NULL COMMENT '结构定义ID',
    `structure_model`   JSON            NOT NULL COMMENT '结构模型',
    `generation_config` JSON            NOT NULL COMMENT '生成配置',
    `field_overrides`   JSON            NOT NULL COMMENT '字段覆盖',
    `artifacts`         JSON            NOT NULL COMMENT '草稿产物',
    `warnings`          JSON            NOT NULL COMMENT '推断告警',
    `validation_mode`   VARCHAR(16)     NOT NULL COMMENT '校验模式',
    `sample_digest`     VARCHAR(64)     NOT NULL COMMENT '样例摘要',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_structure_draft_definition` (`tenant_id`, `definition_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='结构草稿';

CREATE TABLE `structure_version`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '结构版本ID',
    `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `definition_id`     BIGINT UNSIGNED NOT NULL COMMENT '结构定义ID',
    `version_no`        INT             NOT NULL COMMENT '版本号',
    `structure_model`   JSON            NOT NULL COMMENT '结构模型',
    `generation_config` JSON            NOT NULL COMMENT '生成配置',
    `field_overrides`   JSON            NOT NULL COMMENT '字段覆盖',
    `artifacts`         JSON            NOT NULL COMMENT '版本产物',
    `warnings`          JSON            NOT NULL COMMENT '推断告警',
    `validation_mode`   VARCHAR(16)     NOT NULL COMMENT '校验模式',
    `sample_digest`     VARCHAR(64)     NOT NULL COMMENT '样例摘要',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_structure_version_no` (`tenant_id`, `definition_id`, `version_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='结构版本';

CREATE TABLE `structure_artifact`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '结构产物ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `definition_id` BIGINT UNSIGNED NOT NULL COMMENT '结构定义ID',
    `version_id`    BIGINT UNSIGNED NOT NULL COMMENT '结构版本ID',
    `artifact_key`  VARCHAR(64)     NOT NULL COMMENT '产物类型',
    `artifact_json` JSON            NOT NULL COMMENT '产物内容',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_structure_artifact_key` (`tenant_id`, `version_id`, `artifact_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='结构产物';

CREATE TABLE `structure_publish_audit`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '发布审计ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `definition_id` BIGINT UNSIGNED NOT NULL COMMENT '结构定义ID',
    `version_no`    INT             NOT NULL COMMENT '版本号',
    `operator_id`   BIGINT UNSIGNED NULL COMMENT '操作人ID',
    `status`        VARCHAR(16)     NOT NULL COMMENT '发布状态',
    `summary`       VARCHAR(512)    NULL COMMENT '摘要',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_structure_publish_audit_definition` (`tenant_id`, `definition_id`, `version_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='结构发布审计';
