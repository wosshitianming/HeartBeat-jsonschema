CREATE TABLE `mp_account`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公众号账号ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name` VARCHAR(128) NULL COMMENT '账号名称', `app_id` VARCHAR(128) NULL COMMENT '公众号AppID', `app_secret` VARCHAR(512) NULL COMMENT '公众号AppSecret',
    `token` VARCHAR(128) NULL COMMENT '消息校验Token', `aes_key` VARCHAR(256) NULL COMMENT '消息加解密密钥', `status` VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_mp_account_app` (`tenant_id`, `app_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号账号';

CREATE TABLE `mp_menu`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公众号菜单ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `account_id` BIGINT UNSIGNED NULL COMMENT '公众号账号ID', `parent_id` BIGINT UNSIGNED NULL COMMENT '父菜单ID', `name` VARCHAR(128) NULL COMMENT '菜单名称',
    `menu_type` VARCHAR(32) NULL COMMENT '菜单类型', `url` VARCHAR(512) NULL COMMENT '跳转地址', `payload` JSON NULL COMMENT '菜单载荷',
    `sort_no` INT NULL COMMENT '排序号', `status` VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号菜单';

CREATE TABLE `mp_material`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公众号素材ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `account_id` BIGINT UNSIGNED NULL COMMENT '公众号账号ID', `material_type` VARCHAR(32) NULL COMMENT '素材类型', `title` VARCHAR(128) NULL COMMENT '素材标题',
    `media_id` VARCHAR(128) NULL COMMENT '媒体ID', `url` VARCHAR(512) NULL COMMENT '素材地址', `payload` JSON NULL COMMENT '素材载荷', `status` VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号素材';

CREATE TABLE `mp_auto_reply`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自动回复ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `account_id` BIGINT UNSIGNED NULL COMMENT '公众号账号ID', `keyword` VARCHAR(128) NULL COMMENT '匹配关键字', `match_type` VARCHAR(16) NULL COMMENT '匹配方式',
    `reply_type` VARCHAR(32) NULL COMMENT '回复类型', `reply_content` TEXT NULL COMMENT '回复内容', `sort_no` INT NULL COMMENT '排序号', `status` VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号自动回复';

CREATE TABLE `mp_sync_log`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公众号同步日志ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `account_id` BIGINT UNSIGNED NULL COMMENT '公众号账号ID', `sync_type` VARCHAR(32) NOT NULL COMMENT '同步类型', `status` VARCHAR(16) NOT NULL COMMENT '同步状态',
    `message` VARCHAR(512) NULL COMMENT '同步消息', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号同步日志';

CREATE TABLE `report_datasource`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表数据源ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name` VARCHAR(128) NULL COMMENT '数据源名称', `datasource_key` VARCHAR(128) NULL COMMENT '数据源标识', `config_json` JSON NULL COMMENT '数据源配置',
    `status` VARCHAR(16) NULL COMMENT '状态', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表数据源';

CREATE TABLE `report_dataset`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表数据集ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name` VARCHAR(128) NULL COMMENT '数据集名称', `dataset_key` VARCHAR(128) NULL COMMENT '数据集标识', `query_sql` TEXT NULL COMMENT '查询SQL',
    `params_json` JSON NULL COMMENT '查询参数', `status` VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_report_dataset_key` (`tenant_id`, `dataset_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表数据集';

CREATE TABLE `report_template`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表模板ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `dataset_id` BIGINT UNSIGNED NULL COMMENT '报表数据集ID', `name` VARCHAR(128) NULL COMMENT '模板名称', `template_key` VARCHAR(128) NULL COMMENT '模板标识',
    `template_json` JSON NULL COMMENT '模板配置', `status` VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表模板';

CREATE TABLE `report_query_log`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表查询日志ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `dataset_id` BIGINT UNSIGNED NULL COMMENT '报表数据集ID', `params_json` JSON NULL COMMENT '查询参数', `row_count` INT NULL COMMENT '结果行数',
    `status` VARCHAR(16) NULL COMMENT '查询状态', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表查询日志';

CREATE TABLE `report_export_task`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表导出任务ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `template_id` BIGINT UNSIGNED NULL COMMENT '报表模板ID', `status` VARCHAR(16) NOT NULL COMMENT '导出状态', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表导出任务';

CREATE TABLE `mobile_app`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '移动应用ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name` VARCHAR(128) NULL COMMENT '应用名称', `app_key` VARCHAR(128) NULL COMMENT '应用标识', `entry_url` VARCHAR(512) NULL COMMENT '入口地址',
    `status` VARCHAR(16) NULL COMMENT '状态', `config_json` JSON NULL COMMENT '应用配置',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_mobile_app_key` (`tenant_id`, `app_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='移动应用';

CREATE TABLE `mobile_app_version`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '移动应用版本ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `app_id` BIGINT UNSIGNED NOT NULL COMMENT '移动应用ID', `version_no` INT NOT NULL COMMENT '版本号', `schema_json` JSON NULL COMMENT '应用Schema',
    `status` VARCHAR(16) NOT NULL COMMENT '状态', `published_at` DATETIME(3) NULL COMMENT '发布时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_mobile_app_version_no` (`tenant_id`, `app_id`, `version_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='移动应用版本';

CREATE TABLE `mobile_page`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '移动页面ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `app_id` BIGINT UNSIGNED NULL COMMENT '移动应用ID', `name` VARCHAR(128) NULL COMMENT '页面名称', `page_key` VARCHAR(128) NULL COMMENT '页面标识',
    `route_path` VARCHAR(256) NULL COMMENT '路由路径', `schema_json` JSON NULL COMMENT '页面Schema', `sort_no` INT NULL COMMENT '排序号', `status` VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='移动页面';

CREATE TABLE `mobile_api_route`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '移动接口路由ID', `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `app_id` BIGINT UNSIGNED NULL COMMENT '移动应用ID', `name` VARCHAR(128) NULL COMMENT '路由名称', `route_key` VARCHAR(128) NULL COMMENT '路由标识',
    `method` VARCHAR(16) NULL COMMENT 'HTTP方法', `path` VARCHAR(256) NULL COMMENT '请求路径', `target_url` VARCHAR(512) NULL COMMENT '目标地址',
    `sort_no` INT NULL COMMENT '排序号', `status` VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='移动接口路由';
