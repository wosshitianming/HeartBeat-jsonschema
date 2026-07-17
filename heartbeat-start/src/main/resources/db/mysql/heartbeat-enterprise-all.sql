-- Generated from Flyway migrations. Do not edit manually.
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- =================================================================
-- V1__enterprise_platform_iam_auth.sql
-- =================================================================
SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE `sys_tenant_plan`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
    `plan_code`         VARCHAR(64)     NOT NULL COMMENT '套餐编码',
    `plan_name`         VARCHAR(128)    NOT NULL COMMENT '套餐名称',
    `plan_type`         VARCHAR(32)     NOT NULL DEFAULT 'STANDARD' COMMENT '套餐类型',
    `description`       VARCHAR(512)    NULL COMMENT '套餐说明',
    `max_user_count`    INT             NULL COMMENT '最大用户数',
    `max_storage_mb`    BIGINT          NULL COMMENT '最大存储容量MB',
    `feature_policy`    JSON            NULL COMMENT '功能策略',
    `status`            VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `sort_no`           INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `version`           INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_plan_code` (`plan_code`, `delete_marker`),
    KEY `idx_tenant_plan_status` (`status`, `sort_no`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='租户套餐';

CREATE TABLE `sys_plan_feature`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '套餐功能ID',
    `plan_id`        BIGINT UNSIGNED NOT NULL COMMENT '套餐ID',
    `feature_code`   VARCHAR(96)     NOT NULL COMMENT '功能编码',
    `feature_name`   VARCHAR(128)    NOT NULL COMMENT '功能名称',
    `feature_group`  VARCHAR(64)     NULL COMMENT '功能分组',
    `feature_type`   VARCHAR(32)     NOT NULL DEFAULT 'BOOLEAN' COMMENT '功能类型',
    `quota_limit`    BIGINT          NULL COMMENT '功能额度',
    `enabled`        TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `config_schema`  JSON            NULL COMMENT '配置Schema',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `sort_no`        INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_feature_code` (`plan_id`, `feature_code`),
    KEY `idx_plan_feature_plan` (`plan_id`, `status`, `sort_no`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='套餐功能';

CREATE TABLE `sys_tenant`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    `plan_id`        BIGINT UNSIGNED NULL COMMENT '套餐ID',
    `tenant_code`    VARCHAR(64)     NOT NULL COMMENT '租户编码',
    `tenant_name`    VARCHAR(128)    NOT NULL COMMENT '租户名称',
    `tenant_type`    VARCHAR(32)     NOT NULL DEFAULT 'ENTERPRISE' COMMENT '租户类型',
    `domain`         VARCHAR(128)    NULL COMMENT '租户域名',
    `contact_name`   VARCHAR(64)     NULL COMMENT '联系人',
    `contact_phone`  VARCHAR(32)     NULL COMMENT '联系人手机号',
    `contact_email`  VARCHAR(128)    NULL COMMENT '联系人邮箱',
    `logo_url`       VARCHAR(512)    NULL COMMENT 'Logo地址',
    `timezone`       VARCHAR(64)     NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '时区',
    `locale`         VARCHAR(32)     NOT NULL DEFAULT 'zh-CN' COMMENT '语言区域',
    `expire_at`      DATETIME(3)     NULL COMMENT '到期时间',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`, `delete_marker`),
    UNIQUE KEY `uk_tenant_domain` (`domain`, `delete_marker`),
    KEY `idx_tenant_plan` (`plan_id`, `status`),
    KEY `idx_tenant_expire` (`expire_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='租户';

CREATE TABLE `sys_tenant_feature`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '租户功能ID',
    `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `feature_code` VARCHAR(96)     NOT NULL COMMENT '功能编码',
    `feature_name` VARCHAR(128)    NOT NULL COMMENT '功能名称',
    `enabled`      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `quota_limit`  BIGINT          NULL COMMENT '功能额度',
    `config_json`  JSON            NULL COMMENT '功能配置',
    `effective_at` DATETIME(3)     NULL COMMENT '生效时间',
    `expire_at`    DATETIME(3)     NULL COMMENT '到期时间',
    `version`      INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_feature_code` (`tenant_id`, `feature_code`),
    KEY `idx_tenant_feature_enabled` (`tenant_id`, `enabled`, `expire_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='租户功能';

CREATE TABLE `sys_dept`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `parent_id`      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父部门ID',
    `dept_code`      VARCHAR(64)     NOT NULL COMMENT '部门编码',
    `dept_name`      VARCHAR(128)    NOT NULL COMMENT '部门名称',
    `ancestors`      VARCHAR(512)    NOT NULL DEFAULT '0' COMMENT '祖级列表',
    `dept_level`     INT             NOT NULL DEFAULT 1 COMMENT '部门层级',
    `leader_user_id` BIGINT UNSIGNED NULL COMMENT '负责人用户ID',
    `phone`          VARCHAR(32)     NULL COMMENT '联系电话',
    `email`          VARCHAR(128)    NULL COMMENT '邮箱',
    `sort_no`        INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_code` (`tenant_id`, `dept_code`, `delete_marker`),
    KEY `idx_dept_parent` (`tenant_id`, `parent_id`, `sort_no`, `id`),
    KEY `idx_dept_leader` (`tenant_id`, `leader_user_id`),
    KEY `idx_dept_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='部门';

CREATE TABLE `sys_post`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `post_code`      VARCHAR(64)     NOT NULL COMMENT '岗位编码',
    `post_name`      VARCHAR(128)    NOT NULL COMMENT '岗位名称',
    `post_type`      VARCHAR(32)     NOT NULL DEFAULT 'BUSINESS' COMMENT '岗位类型',
    `description`    VARCHAR(512)    NULL COMMENT '岗位说明',
    `sort_no`        INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_code` (`tenant_id`, `post_code`, `delete_marker`),
    KEY `idx_post_status` (`tenant_id`, `status`, `sort_no`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='岗位';

CREATE TABLE `sys_user`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `tenant_id`           BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `dept_id`             BIGINT UNSIGNED NULL COMMENT '主部门ID',
    `username`            VARCHAR(64)     NOT NULL COMMENT '用户名',
    `nickname`            VARCHAR(128)    NULL COMMENT '昵称',
    `real_name`           VARCHAR(128)    NULL COMMENT '真实姓名',
    `email`               VARCHAR(128)    NULL COMMENT '邮箱',
    `phone`               VARCHAR(32)     NULL COMMENT '手机号',
    `avatar_url`          VARCHAR(512)    NULL COMMENT '头像',
    `password_hash`       VARCHAR(255)    NOT NULL COMMENT '密码哈希',
    `password_algo`       VARCHAR(32)     NOT NULL DEFAULT 'BCRYPT' COMMENT '密码算法',
    `password_updated_at` DATETIME(3)     NULL COMMENT '密码更新时间',
    `gender`              VARCHAR(16)     NULL COMMENT '性别',
    `user_type`           VARCHAR(32)     NOT NULL DEFAULT 'NORMAL' COMMENT '用户类型',
    `status`              VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `last_login_at`       DATETIME(3)     NULL COMMENT '最后登录时间',
    `last_login_ip`       VARCHAR(64)     NULL COMMENT '最后登录IP',
    `version`             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`tenant_id`, `username`, `delete_marker`),
    UNIQUE KEY `uk_user_email` (`tenant_id`, `email`, `delete_marker`),
    UNIQUE KEY `uk_user_phone` (`tenant_id`, `phone`, `delete_marker`),
    KEY `idx_user_dept` (`tenant_id`, `dept_id`, `status`),
    KEY `idx_user_status` (`tenant_id`, `status`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户';

CREATE TABLE `sys_user_post`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户岗位关系ID',
    `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `post_id`      BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
    `primary_post` TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否主岗位',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`tenant_id`, `user_id`, `post_id`),
    KEY `idx_user_post_post` (`tenant_id`, `post_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户岗位关系';

CREATE TABLE `sys_role`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `role_code`      VARCHAR(64)     NOT NULL COMMENT '角色编码',
    `role_name`      VARCHAR(128)    NOT NULL COMMENT '角色名称',
    `role_type`      VARCHAR(32)     NOT NULL DEFAULT 'BUSINESS' COMMENT '角色类型',
    `data_scope`     VARCHAR(32)     NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围',
    `description`    VARCHAR(512)    NULL COMMENT '角色说明',
    `sort_no`        INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`tenant_id`, `role_code`, `delete_marker`),
    KEY `idx_role_status` (`tenant_id`, `status`, `sort_no`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色';

CREATE TABLE `sys_permission`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `permission_code` VARCHAR(128)    NOT NULL COMMENT '权限编码',
    `permission_name` VARCHAR(128)    NOT NULL COMMENT '权限名称',
    `permission_type` VARCHAR(32)     NOT NULL DEFAULT 'API' COMMENT '权限类型',
    `resource_type`   VARCHAR(32)     NOT NULL DEFAULT 'HTTP_API' COMMENT '资源类型',
    `resource_path`   VARCHAR(256)    NULL COMMENT '资源路径',
    `http_method`     VARCHAR(16)     NULL COMMENT 'HTTP方法',
    `description`     VARCHAR(512)    NULL COMMENT '权限说明',
    `status`          VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `sort_no`         INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `version`         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`tenant_id`, `permission_code`, `delete_marker`),
    KEY `idx_permission_resource` (`tenant_id`, `resource_type`, `resource_path`, `http_method`),
    KEY `idx_permission_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='权限';

CREATE TABLE `sys_menu`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `parent_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    `menu_code`       VARCHAR(64)     NOT NULL COMMENT '菜单编码',
    `menu_name`       VARCHAR(128)    NOT NULL COMMENT '菜单名称',
    `menu_type`       VARCHAR(32)     NOT NULL DEFAULT 'MENU' COMMENT '菜单类型',
    `route_path`      VARCHAR(256)    NULL COMMENT '路由路径',
    `component_path`  VARCHAR(256)    NULL COMMENT '组件路径',
    `redirect_path`   VARCHAR(256)    NULL COMMENT '重定向路径',
    `icon`            VARCHAR(128)    NULL COMMENT '图标',
    `visible`         TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否可见',
    `keep_alive`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否缓存',
    `external_link`   VARCHAR(512)    NULL COMMENT '外链地址',
    `permission_mode` VARCHAR(32)     NOT NULL DEFAULT 'RELATION' COMMENT '权限绑定模式',
    `sort_no`         INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`          VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_code` (`tenant_id`, `menu_code`, `delete_marker`),
    KEY `idx_menu_parent` (`tenant_id`, `parent_id`, `sort_no`, `id`),
    KEY `idx_menu_status` (`tenant_id`, `status`, `visible`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='菜单';

CREATE TABLE `sys_user_role`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户角色关系ID',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `role_id`    BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`tenant_id`, `user_id`, `role_id`),
    KEY `idx_user_role_role` (`tenant_id`, `role_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户角色关系';

CREATE TABLE `sys_role_permission`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色权限关系ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `role_id`       BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`tenant_id`, `role_id`, `permission_id`),
    KEY `idx_role_permission_permission` (`tenant_id`, `permission_id`, `role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色权限关系';

CREATE TABLE `sys_menu_permission`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单权限关系ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `menu_id`       BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
    `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_permission` (`tenant_id`, `menu_id`, `permission_id`),
    KEY `idx_menu_permission_permission` (`tenant_id`, `permission_id`, `menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='菜单权限关系';

CREATE TABLE `sys_role_dept`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色部门关系ID',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `role_id`    BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `dept_id`    BIGINT UNSIGNED NOT NULL COMMENT '部门ID',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_dept` (`tenant_id`, `role_id`, `dept_id`),
    KEY `idx_role_dept_dept` (`tenant_id`, `dept_id`, `role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色部门数据权限关系';

CREATE TABLE `sys_dict_type`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `dict_code`      VARCHAR(64)     NOT NULL COMMENT '字典编码',
    `dict_name`      VARCHAR(128)    NOT NULL COMMENT '字典名称',
    `description`    VARCHAR(512)    NULL COMMENT '字典说明',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `sort_no`        INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_type_code` (`tenant_id`, `dict_code`, `delete_marker`),
    KEY `idx_dict_type_status` (`tenant_id`, `status`, `sort_no`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='字典类型';

CREATE TABLE `sys_dict_item`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '字典项ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `dict_type_id`   BIGINT UNSIGNED NOT NULL COMMENT '字典类型ID',
    `item_label`     VARCHAR(128)    NOT NULL COMMENT '字典项标签',
    `item_value`     VARCHAR(128)    NOT NULL COMMENT '字典项值',
    `item_color`     VARCHAR(32)     NULL COMMENT '显示颜色',
    `item_css_class` VARCHAR(128)    NULL COMMENT '样式类',
    `default_flag`   TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否默认',
    `description`    VARCHAR(512)    NULL COMMENT '字典项说明',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `sort_no`        INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_item_value` (`tenant_id`, `dict_type_id`, `item_value`, `delete_marker`),
    KEY `idx_dict_item_type` (`tenant_id`, `dict_type_id`, `status`, `sort_no`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='字典项';

CREATE TABLE `sys_config`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `config_key`     VARCHAR(128)    NOT NULL COMMENT '配置键',
    `config_name`    VARCHAR(128)    NOT NULL COMMENT '配置名称',
    `config_value`   TEXT            NULL COMMENT '配置值',
    `value_type`     VARCHAR(32)     NOT NULL DEFAULT 'STRING' COMMENT '值类型',
    `encrypted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否加密',
    `config_group`   VARCHAR(64)     NOT NULL DEFAULT 'system' COMMENT '配置分组',
    `description`    VARCHAR(512)    NULL COMMENT '配置说明',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`tenant_id`, `config_key`, `delete_marker`),
    KEY `idx_config_group` (`tenant_id`, `config_group`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='系统配置';

CREATE TABLE `sys_notice`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知公告ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `notice_title`   VARCHAR(200)    NOT NULL COMMENT '公告标题',
    `notice_type`    VARCHAR(32)     NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型',
    `notice_content` TEXT            NULL COMMENT '公告内容',
    `publish_scope`  VARCHAR(32)     NOT NULL DEFAULT 'ALL' COMMENT '发布范围',
    `publish_status` VARCHAR(32)     NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态',
    `published_at`   DATETIME(3)     NULL COMMENT '发布时间',
    `expired_at`     DATETIME(3)     NULL COMMENT '过期时间',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_notice_status` (`tenant_id`, `publish_status`, `published_at`),
    KEY `idx_notice_expire` (`tenant_id`, `expired_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知公告';

CREATE TABLE `sys_user_preference`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户偏好ID',
    `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `preference_key`   VARCHAR(128)    NOT NULL COMMENT '偏好键',
    `preference_value` TEXT            NULL COMMENT '偏好值',
    `value_type`       VARCHAR(32)     NOT NULL DEFAULT 'JSON' COMMENT '值类型',
    `version`          INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_preference` (`tenant_id`, `user_id`, `preference_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户偏好';

CREATE TABLE `auth_oauth_client`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'OAuth客户端ID',
    `tenant_id`           BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `client_id`           VARCHAR(128)    NOT NULL COMMENT '客户端标识',
    `client_name`         VARCHAR(128)    NOT NULL COMMENT '客户端名称',
    `client_secret_hash`  VARCHAR(255)    NOT NULL COMMENT '客户端密钥哈希',
    `client_type`         VARCHAR(32)     NOT NULL DEFAULT 'CONFIDENTIAL' COMMENT '客户端类型',
    `access_token_ttl`    INT             NOT NULL DEFAULT 7200 COMMENT '访问令牌TTL秒',
    `refresh_token_ttl`   INT             NOT NULL DEFAULT 604800 COMMENT '刷新令牌TTL秒',
    `scopes`              VARCHAR(512)    NULL COMMENT '授权范围',
    `auto_approve`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否自动授权',
    `status`              VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_client_id` (`tenant_id`, `client_id`, `delete_marker`),
    KEY `idx_oauth_client_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='OAuth客户端';

CREATE TABLE `auth_client_grant`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '客户端授权方式ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `oauth_client_id` BIGINT UNSIGNED NOT NULL COMMENT 'OAuth客户端ID',
    `grant_type`      VARCHAR(64)     NOT NULL COMMENT '授权方式',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_grant` (`tenant_id`, `oauth_client_id`, `grant_type`),
    KEY `idx_client_grant_client` (`tenant_id`, `oauth_client_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='OAuth客户端授权方式';

CREATE TABLE `auth_client_redirect_uri`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '客户端回调地址ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `oauth_client_id` BIGINT UNSIGNED NOT NULL COMMENT 'OAuth客户端ID',
    `redirect_uri`    VARCHAR(512)    NOT NULL COMMENT '回调地址',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_redirect_uri` (`tenant_id`, `oauth_client_id`, `redirect_uri`),
    KEY `idx_client_redirect_client` (`tenant_id`, `oauth_client_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='OAuth客户端回调地址';

CREATE TABLE `auth_social_provider`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '社交登录提供方ID',
    `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `provider_code`     VARCHAR(64)     NOT NULL COMMENT '提供方编码',
    `provider_name`     VARCHAR(128)    NOT NULL COMMENT '提供方名称',
    `provider_type`     VARCHAR(32)     NOT NULL DEFAULT 'OAUTH2' COMMENT '提供方类型',
    `client_id`         VARCHAR(128)    NULL COMMENT '第三方客户端ID',
    `app_key`           VARCHAR(128)    NULL COMMENT '第三方AppKey',
    `app_secret_cipher` VARCHAR(512)    NULL COMMENT '第三方密钥密文',
    `authorize_url`     VARCHAR(512)    NULL COMMENT '授权地址',
    `token_url`         VARCHAR(512)    NULL COMMENT '令牌地址',
    `user_info_url`     VARCHAR(512)    NULL COMMENT '用户信息地址',
    `scopes`            VARCHAR(512)    NULL COMMENT '授权范围',
    `enabled`           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `status`            VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `version`           INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `delete_marker`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_social_provider_code` (`tenant_id`, `provider_code`, `delete_marker`),
    KEY `idx_social_provider_enabled` (`tenant_id`, `enabled`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='社交登录提供方';

CREATE TABLE `auth_social_binding`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '社交账号绑定ID',
    `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `provider_id`       BIGINT UNSIGNED NOT NULL COMMENT '提供方ID',
    `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `external_user_id`  VARCHAR(128)    NOT NULL COMMENT '第三方用户ID',
    `external_union_id` VARCHAR(128)    NULL COMMENT '第三方统一ID',
    `external_username` VARCHAR(128)    NULL COMMENT '第三方用户名',
    `external_avatar`   VARCHAR(512)    NULL COMMENT '第三方头像',
    `binding_status`    VARCHAR(16)     NOT NULL DEFAULT 'BOUND' COMMENT '绑定状态',
    `bound_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '绑定时间',
    `last_login_at`     DATETIME(3)     NULL COMMENT '最后登录时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_social_binding_external` (`tenant_id`, `provider_id`, `external_user_id`),
    UNIQUE KEY `uk_social_binding_user` (`tenant_id`, `provider_id`, `user_id`),
    KEY `idx_social_binding_union` (`tenant_id`, `provider_id`, `external_union_id`),
    KEY `idx_social_binding_user_status` (`tenant_id`, `user_id`, `binding_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='社交账号绑定';

CREATE TABLE `auth_session`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话记录ID',
    `tenant_id`           BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `session_id`          VARCHAR(64)     NOT NULL COMMENT '会话ID',
    `user_id`             BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `access_token_jti`    VARCHAR(96)     NULL COMMENT '访问令牌JTI',
    `refresh_token_hash`  VARCHAR(255)    NULL COMMENT '刷新令牌哈希',
    `device_type`         VARCHAR(32)     NULL COMMENT '设备类型',
    `device_name`         VARCHAR(128)    NULL COMMENT '设备名称',
    `login_ip`            VARCHAR(64)     NULL COMMENT '登录IP',
    `user_agent`          VARCHAR(512)    NULL COMMENT 'User-Agent',
    `status`              VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
    `issued_at`           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '签发时间',
    `expire_at`           DATETIME(3)     NOT NULL COMMENT '访问令牌过期时间',
    `refresh_expire_at`   DATETIME(3)     NULL COMMENT '刷新令牌过期时间',
    `revoked_at`          DATETIME(3)     NULL COMMENT '撤销时间',
    `last_access_at`      DATETIME(3)     NULL COMMENT '最后访问时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auth_session_no` (`tenant_id`, `session_id`),
    KEY `idx_auth_session_user` (`tenant_id`, `user_id`, `status`, `expire_at`),
    KEY `idx_auth_session_refresh` (`tenant_id`, `refresh_expire_at`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='认证会话';

CREATE TABLE `sys_oper_log`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
    `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `trace_id`         VARCHAR(96)     NULL COMMENT '链路追踪ID',
    `module_code`      VARCHAR(64)     NULL COMMENT '模块编码',
    `operation_type`   VARCHAR(64)     NULL COMMENT '操作类型',
    `operation_name`   VARCHAR(128)    NULL COMMENT '操作名称',
    `operator_id`      BIGINT UNSIGNED NULL COMMENT '操作人ID',
    `operator_name`    VARCHAR(128)    NULL COMMENT '操作人名称',
    `request_method`   VARCHAR(16)     NULL COMMENT '请求方法',
    `request_path`     VARCHAR(256)    NULL COMMENT '请求路径',
    `request_ip`       VARCHAR(64)     NULL COMMENT '请求IP',
    `user_agent`       VARCHAR(512)    NULL COMMENT 'User-Agent',
    `request_params`   TEXT            NULL COMMENT '请求参数摘要',
    `response_summary` TEXT            NULL COMMENT '响应摘要',
    `result_status`    VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS' COMMENT '结果状态',
    `error_code`       VARCHAR(64)     NULL COMMENT '错误码',
    `error_message`    VARCHAR(1024)   NULL COMMENT '错误信息',
    `duration_ms`      BIGINT          NULL COMMENT '耗时毫秒',
    `operated_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_oper_log_operator` (`tenant_id`, `operator_id`, `operated_at`),
    KEY `idx_oper_log_trace` (`tenant_id`, `trace_id`),
    KEY `idx_oper_log_path` (`tenant_id`, `request_path`, `operated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='操作日志';

CREATE TABLE `sys_login_log`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `trace_id`       VARCHAR(96)     NULL COMMENT '链路追踪ID',
    `user_id`        BIGINT UNSIGNED NULL COMMENT '用户ID',
    `username`       VARCHAR(64)     NULL COMMENT '用户名',
    `login_type`     VARCHAR(32)     NOT NULL DEFAULT 'PASSWORD' COMMENT '登录类型',
    `login_ip`       VARCHAR(64)     NULL COMMENT '登录IP',
    `user_agent`     VARCHAR(512)    NULL COMMENT 'User-Agent',
    `result_status`  VARCHAR(16)     NOT NULL COMMENT '登录结果',
    `failure_reason` VARCHAR(512)    NULL COMMENT '失败原因',
    `logged_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '登录时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_login_log_user` (`tenant_id`, `user_id`, `logged_at`),
    KEY `idx_login_log_username` (`tenant_id`, `username`, `logged_at`),
    KEY `idx_login_log_trace` (`tenant_id`, `trace_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='登录日志';

-- =================================================================
-- V2__platform_seed.sql
-- =================================================================
SET NAMES utf8mb4;
SET time_zone = '+00:00';

INSERT INTO `sys_tenant_plan`
(`id`, `plan_code`, `plan_name`, `plan_type`, `description`, `max_user_count`, `max_storage_mb`,
 `feature_policy`, `status`, `sort_no`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`,
 `update_time`)
VALUES
(1, 'ENTERPRISE', 'Enterprise Edition', 'ENTERPRISE', 'Default enterprise plan', 1000, 102400,
 JSON_OBJECT('edition', 'enterprise'), 'ENABLED', 1, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_plan_feature`
(`id`, `plan_id`, `feature_code`, `feature_name`, `feature_group`, `feature_type`, `quota_limit`, `enabled`,
 `config_schema`, `status`, `sort_no`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`,
 `update_time`)
VALUES
(1, 1, 'STRUCTURE_INTELLIGENCE', 'Structure Intelligence', 'platform', 'BOOLEAN', NULL, 1, NULL,
 'ENABLED', 10, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
(2, 1, 'FLOW_AUTOMATION', 'Flow Automation', 'platform', 'BOOLEAN', NULL, 1, NULL,
 'ENABLED', 20, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
(3, 1, 'IAM_AUDIT', 'IAM And Audit', 'platform', 'BOOLEAN', NULL, 1, NULL,
 'ENABLED', 30, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_tenant`
(`id`, `plan_id`, `tenant_code`, `tenant_name`, `tenant_type`, `domain`, `contact_name`, `contact_phone`,
 `contact_email`, `logo_url`, `timezone`, `locale`, `expire_at`, `status`, `version`, `delete_marker`,
 `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 1, 'heartbeat', 'HeartBeat Platform', 'ENTERPRISE', NULL, 'Platform Administrator', NULL,
 NULL, NULL, 'Asia/Shanghai', 'zh-CN', NULL, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_tenant_feature`
(`id`, `tenant_id`, `feature_code`, `feature_name`, `enabled`, `quota_limit`, `config_json`, `effective_at`,
 `expire_at`, `version`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (1, 1, 'STRUCTURE_INTELLIGENCE', 'Structure Intelligence', 1, NULL, 0, CURRENT_TIMESTAMP, NULL,
        0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (2, 1, 'FLOW_AUTOMATION', 'Flow Automation', 1, NULL, 0, CURRENT_TIMESTAMP, NULL,
        0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (3, 1, 'IAM_AUDIT', 'IAM And Audit', 1, NULL, 0, CURRENT_TIMESTAMP, NULL,
        0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_dept`
(`id`, `tenant_id`, `parent_id`, `dept_code`, `dept_name`, `ancestors`, `dept_level`, `leader_user_id`,
 `phone`, `email`, `sort_no`, `status`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`,
 `update_time`)
VALUES
(1, 1, 0, 'platform', 'Platform Root Department', '0', 1, 1, NULL, NULL, 1, 'ENABLED', 0, 0,
 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_post`
(`id`, `tenant_id`, `post_code`, `post_name`, `post_type`, `description`, `sort_no`, `status`,
 `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 1, 'platform-admin', 'Platform Administrator', 'MANAGEMENT', 'Default platform administrator post',
 1, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_user`
(`id`, `tenant_id`, `dept_id`, `username`, `nickname`, `real_name`, `email`, `phone`, `avatar_url`,
 `password_hash`, `password_algo`, `password_updated_at`, `gender`, `user_type`, `status`,
 `last_login_at`, `last_login_ip`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 1, 1, 'admin', 'Administrator', 'Platform Administrator', NULL, NULL, NULL,
 '$2a$10$CwTycUXWue0Thq9StjUM0uJ8.7o6iOJIsv4u4tIKu3sZvK7N5Sx9e', 'BCRYPT', CURRENT_TIMESTAMP(3),
 NULL, 'SUPER_ADMIN', 'ENABLED', NULL, NULL, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_user_post`
(`id`, `tenant_id`, `user_id`, `post_id`, `primary_post`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (1, 1, 1, 1, 1, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_role`
(`id`, `tenant_id`, `role_code`, `role_name`, `role_type`, `data_scope`, `description`, `sort_no`, `status`,
 `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 1, 'super_admin', 'Super Administrator', 'SYSTEM', 'ALL', 'Built-in platform super administrator role',
 1, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_user_role`
(`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (1, 1, 1, 1, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_config`
(`id`, `tenant_id`, `config_key`, `config_name`, `config_value`, `value_type`, `encrypted`, `config_group`,
 `description`, `status`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 1, 'system.name', 'System Name', 'HeartBeat', 'STRING', 0, 'system', 'Default system display name',
 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

-- =================================================================
-- V3__permission_seed.sql
-- =================================================================
SET NAMES utf8mb4;
SET time_zone = '+00:00';

INSERT INTO `sys_menu`
(`id`, `tenant_id`, `parent_id`, `menu_code`, `menu_name`, `menu_type`, `route_path`, `component_path`,
 `redirect_path`, `icon`, `visible`, `keep_alive`, `external_link`, `permission_mode`, `sort_no`, `status`,
 `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (1, 1, 0, 'dashboard', 'Dashboard', 'MENU', '/dashboard', 'dashboard/index', NULL, 'dashboard', 1, 1, NULL,
        'RELATION', 1, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (10, 1, 0, 'root:system', 'System', 'CATALOG', '/system', NULL, NULL, 'system', 1, 0, NULL, 'RELATION', 10,
        'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (11, 1, 10, 'system:tenant', 'Tenants', 'MENU', '/system/tenant', 'system/tenant/index', NULL, 'tenant', 1, 0,
        NULL, 'RELATION', 11, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (12, 1, 10, 'system:user', 'Users', 'MENU', '/system/user', 'system/user/index', NULL, 'user', 1, 0, NULL,
        'RELATION', 12, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (13, 1, 10, 'system:dept', 'Departments', 'MENU', '/system/dept', 'system/dept/index', NULL, 'dept', 1, 0, NULL,
        'RELATION', 13, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (14, 1, 10, 'system:post', 'Posts', 'MENU', '/system/post', 'system/post/index', NULL, 'post', 1, 0, NULL,
        'RELATION', 14, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (15, 1, 10, 'system:role', 'Roles', 'MENU', '/system/role', 'system/role/index', NULL, 'role', 1, 0, NULL,
        'RELATION', 15, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (16, 1, 10, 'system:menu', 'Menus', 'MENU', '/system/menu', 'system/menu/index', NULL, 'menu', 1, 0, NULL,
        'RELATION', 16, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (17, 1, 10, 'system:dict', 'Dictionaries', 'MENU', '/system/dict', 'system/dict/index', NULL, 'dict', 1, 0, NULL,
        'RELATION', 17, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (18, 1, 10, 'system:config', 'Configurations', 'MENU', '/system/config', 'system/config/index', NULL, 'config',
        1, 0, NULL, 'RELATION', 18, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (19, 1, 10, 'system:notice', 'Notices', 'MENU', '/system/notice', 'system/notice/index', NULL, 'notice', 1, 0,
        NULL, 'RELATION', 19, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (20, 1, 10, 'system:oauth', 'OAuth Clients', 'MENU', '/system/oauth', 'system/oauth/index', NULL, 'oauth', 1, 0,
        NULL, 'RELATION', 20, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (21, 1, 10, 'system:social', 'Social Providers', 'MENU', '/system/social', 'system/social/index', NULL, 'social',
        1, 0, NULL, 'RELATION', 21, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (22, 1, 10, 'monitor:operlog', 'Operation Logs', 'MENU', '/system/audit/operations', 'system/audit/operations',
        NULL, 'audit', 1, 0, NULL, 'RELATION', 22, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (23, 1, 10, 'monitor:loginlog', 'Login Logs', 'MENU', '/system/audit/logins', 'system/audit/logins', NULL,
        'login-log', 1, 0, NULL, 'RELATION', 23, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (24, 1, 10, 'monitor:online', 'Online Sessions', 'MENU', '/system/sessions', 'system/sessions/index', NULL,
        'session', 1, 0, NULL, 'RELATION', 24, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (30, 1, 0, 'structure:definition', 'Structure Intelligence', 'MENU', '/structure-definitions',
        'structure/definition/index', NULL, 'schema', 1, 1, NULL, 'RELATION', 30, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP,
        0, CURRENT_TIMESTAMP),
       (40, 1, 0, 'root:flow', 'Automation', 'CATALOG', '/flows', NULL, NULL, 'flow', 1, 0, NULL, 'RELATION', 40,
        'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (41, 1, 40, 'flow:studio', 'Flow Studio', 'MENU', '/flows/studio', 'flow/studio/index', NULL, 'flow-studio', 1,
        1, NULL, 'RELATION', 41, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (42, 1, 40, 'flow:definition', 'Flow Definitions', 'MENU', '/flows/definitions', 'flow/definition/index', NULL,
        'flow-definition', 1, 1, NULL, 'RELATION', 42, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (43, 1, 40, 'flow:component', 'Flow Components', 'MENU', '/flows/components', 'flow/component/index', NULL,
        'component', 1, 0, NULL, 'RELATION', 43, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (50, 1, 0, 'root:business', 'Business', 'CATALOG', '/business', NULL, NULL, 'business', 1, 0, NULL, 'RELATION',
        50, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (51, 1, 50, 'biz:workflow', 'Workflow', 'MENU', '/workflow', 'workflow/index', NULL, 'workflow', 1, 1, NULL,
        'RELATION', 51, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (52, 1, 50, 'biz:pay', 'Payment', 'MENU', '/pay', 'pay/index', NULL, 'pay', 1, 0, NULL, 'RELATION', 52,
        'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (53, 1, 50, 'biz:mp', 'Official Account', 'MENU', '/mp', 'mp/index', NULL, 'mp', 1, 0, NULL, 'RELATION', 53,
        'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (54, 1, 50, 'biz:report', 'Reports', 'MENU', '/report', 'report/index', NULL, 'report', 1, 0, NULL, 'RELATION',
        54, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (55, 1, 50, 'biz:mobile', 'Mobile Builder', 'MENU', '/mobile', 'mobile/index', NULL, 'mobile', 1, 0, NULL,
        'RELATION', 55, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (60, 1, 0, 'root:monitor', 'Monitor', 'CATALOG', '/monitor', NULL, NULL, 'monitor', 1, 0, NULL, 'RELATION', 60,
        'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (61, 1, 60, 'monitor:server', 'Server Monitor', 'MENU', '/monitor/server', 'monitor/server/index', NULL,
        'server', 1, 0, NULL, 'RELATION', 61, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (62, 1, 60, 'monitor:cache', 'Cache Monitor', 'MENU', '/monitor/cache', 'monitor/cache/index', NULL, 'cache', 1,
        0, NULL, 'RELATION', 62, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (63, 1, 60, 'monitor:druid', 'Datasource Monitor', 'MENU', '/monitor/druid', 'monitor/druid/index', NULL,
        'datasource', 1, 0, NULL, 'RELATION', 63, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (70, 1, 0, 'root:tool', 'Tools', 'CATALOG', '/tool', NULL, NULL, 'tool', 1, 0, NULL, 'RELATION', 70, 'ENABLED',
        0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (71, 1, 70, 'tool:job', 'Scheduler Jobs', 'MENU', '/tool/jobs', 'tool/job/index', NULL, 'job', 1, 0, NULL,
        'RELATION', 71, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (72, 1, 70, 'tool:gen', 'Code Generator', 'MENU', '/tool/gen', 'tool/gen/index', NULL, 'code', 1, 0, NULL,
        'RELATION', 72, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_permission`
(`tenant_id`, `permission_code`, `permission_name`, `permission_type`, `resource_type`, `resource_path`, `http_method`,
 `description`, `status`, `sort_no`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES (1, 'dashboard:view', 'Dashboard View', 'API', 'HTTP_API', '/api/v1/admin/modules', 'GET',
        'View dashboard and modules', 'ENABLED', 1, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:tenant:list', 'Tenant List', 'API', 'HTTP_API', '/api/v1/admin/resources/tenants', 'GET',
        'List tenants', 'ENABLED', 10, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:tenant:add', 'Tenant Add', 'API', 'HTTP_API', '/api/v1/admin/resources/tenants', 'POST',
        'Create tenants', 'ENABLED', 11, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:tenant:edit', 'Tenant Edit', 'API', 'HTTP_API', '/api/v1/admin/resources/tenants/{id}', 'PUT',
        'Update tenants', 'ENABLED', 12, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:tenant:remove', 'Tenant Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/tenants/{id}', 'DELETE',
        'Remove tenants', 'ENABLED', 13, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:user:list', 'User List', 'API', 'HTTP_API', '/api/v1/users', 'GET', 'List users', 'ENABLED', 20, 0,
        0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:user:add', 'User Add', 'API', 'HTTP_API', '/api/v1/users', 'POST', 'Create users', 'ENABLED', 21, 0,
        0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:user:edit', 'User Edit', 'API', 'HTTP_API', '/api/v1/users/{id}', 'PUT', 'Update users', 'ENABLED',
        22, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:user:remove', 'User Remove', 'API', 'HTTP_API', '/api/v1/users/{id}', 'DELETE', 'Remove users',
        'ENABLED', 23, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:dept:list', 'Department List', 'API', 'HTTP_API', '/api/v1/admin/resources/depts', 'GET',
        'List departments', 'ENABLED', 30, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:dept:add', 'Department Add', 'API', 'HTTP_API', '/api/v1/admin/resources/depts', 'POST',
        'Create departments', 'ENABLED', 31, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:dept:edit', 'Department Edit', 'API', 'HTTP_API', '/api/v1/admin/resources/depts/{id}', 'PUT',
        'Update departments', 'ENABLED', 32, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:dept:remove', 'Department Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/depts/{id}', 'DELETE',
        'Remove departments', 'ENABLED', 33, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:post:list', 'Post List', 'API', 'HTTP_API', '/api/v1/admin/resources/posts', 'GET', 'List posts',
        'ENABLED', 40, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:post:add', 'Post Add', 'API', 'HTTP_API', '/api/v1/admin/resources/posts', 'POST', 'Create posts',
        'ENABLED', 41, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:post:edit', 'Post Edit', 'API', 'HTTP_API', '/api/v1/admin/resources/posts/{id}', 'PUT',
        'Update posts', 'ENABLED', 42, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:post:remove', 'Post Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/posts/{id}', 'DELETE',
        'Remove posts', 'ENABLED', 43, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:role:list', 'Role List', 'API', 'HTTP_API', '/api/v1/iam/roles', 'GET', 'List roles', 'ENABLED', 50,
        0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:role:add', 'Role Add', 'API', 'HTTP_API', '/api/v1/admin/resources/roles', 'POST', 'Create roles',
        'ENABLED', 51, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:role:edit', 'Role Edit', 'API', 'HTTP_API', '/api/v1/admin/resources/roles/{id}', 'PUT',
        'Update roles', 'ENABLED', 52, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:role:remove', 'Role Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/roles/{id}', 'DELETE',
        'Remove roles', 'ENABLED', 53, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:role:grant', 'Role Grant', 'API', 'HTTP_API', '/api/v1/iam/roles/{id}/menus', 'PUT',
        'Grant role menus', 'ENABLED', 54, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:menu:list', 'Menu List', 'API', 'HTTP_API', '/api/v1/iam/menus', 'GET', 'List menus', 'ENABLED', 60,
        0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:menu:add', 'Menu Add', 'API', 'HTTP_API', '/api/v1/iam/menus', 'POST', 'Create menus', 'ENABLED', 61,
        0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:menu:edit', 'Menu Edit', 'API', 'HTTP_API', '/api/v1/iam/menus/{id}', 'PUT', 'Update menus',
        'ENABLED', 62, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:menu:remove', 'Menu Remove', 'API', 'HTTP_API', '/api/v1/iam/menus/{id}', 'DELETE', 'Remove menus',
        'ENABLED', 63, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:dict:list', 'Dictionary List', 'API', 'HTTP_API', '/api/v1/admin/resources/dict-types', 'GET',
        'List dictionaries', 'ENABLED', 70, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:dict:add', 'Dictionary Add', 'API', 'HTTP_API', '/api/v1/admin/resources/dict-types', 'POST',
        'Create dictionaries', 'ENABLED', 71, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:dict:edit', 'Dictionary Edit', 'API', 'HTTP_API', '/api/v1/admin/resources/dict-types/{id}', 'PUT',
        'Update dictionaries', 'ENABLED', 72, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:dict:remove', 'Dictionary Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/dict-types/{id}',
        'DELETE', 'Remove dictionaries', 'ENABLED', 73, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:config:list', 'Configuration List', 'API', 'HTTP_API', '/api/v1/admin/resources/configs', 'GET',
        'List configurations', 'ENABLED', 80, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:config:add', 'Configuration Add', 'API', 'HTTP_API', '/api/v1/admin/resources/configs', 'POST',
        'Create configurations', 'ENABLED', 81, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:config:edit', 'Configuration Edit', 'API', 'HTTP_API', '/api/v1/admin/resources/configs/{id}', 'PUT',
        'Update configurations', 'ENABLED', 82, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:config:remove', 'Configuration Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/configs/{id}',
        'DELETE', 'Remove configurations', 'ENABLED', 83, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:notice:list', 'Notice List', 'API', 'HTTP_API', '/api/v1/admin/resources/notices', 'GET',
        'List notices', 'ENABLED', 90, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:notice:add', 'Notice Add', 'API', 'HTTP_API', '/api/v1/admin/resources/notices', 'POST',
        'Create notices', 'ENABLED', 91, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:notice:edit', 'Notice Edit', 'API', 'HTTP_API', '/api/v1/admin/resources/notices/{id}', 'PUT',
        'Update notices', 'ENABLED', 92, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:notice:remove', 'Notice Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/notices/{id}', 'DELETE',
        'Remove notices', 'ENABLED', 93, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:oauth:list', 'OAuth Client List', 'API', 'HTTP_API', '/api/v1/admin/resources/oauth-clients', 'GET',
        'List OAuth clients', 'ENABLED', 100, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:oauth:add', 'OAuth Client Add', 'API', 'HTTP_API', '/api/v1/admin/resources/oauth-clients', 'POST',
        'Create OAuth clients', 'ENABLED', 101, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:oauth:edit', 'OAuth Client Edit', 'API', 'HTTP_API', '/api/v1/admin/resources/oauth-clients/{id}',
        'PUT', 'Update OAuth clients', 'ENABLED', 102, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:oauth:remove', 'OAuth Client Remove', 'API', 'HTTP_API',
        '/api/v1/admin/resources/oauth-clients/{id}', 'DELETE', 'Remove OAuth clients', 'ENABLED', 103, 0, 0, 0,
        CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:social:list', 'Social Provider List', 'API', 'HTTP_API', '/api/v1/admin/resources/social-providers',
        'GET', 'List social providers', 'ENABLED', 110, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:social:add', 'Social Provider Add', 'API', 'HTTP_API', '/api/v1/admin/resources/social-providers',
        'POST', 'Create social providers', 'ENABLED', 111, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:social:edit', 'Social Provider Edit', 'API', 'HTTP_API',
        '/api/v1/admin/resources/social-providers/{id}', 'PUT', 'Update social providers', 'ENABLED', 112, 0, 0, 0,
        CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'system:social:remove', 'Social Provider Remove', 'API', 'HTTP_API',
        '/api/v1/admin/resources/social-providers/{id}', 'DELETE', 'Remove social providers', 'ENABLED', 113, 0, 0, 0,
        CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'monitor:operlog:list', 'Operation Log List', 'API', 'HTTP_API', '/api/v1/admin/resources/oper-logs', 'GET',
        'List operation logs', 'ENABLED', 120, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'monitor:operlog:remove', 'Operation Log Remove', 'API', 'HTTP_API',
        '/api/v1/admin/resources/oper-logs/{id}', 'DELETE', 'Remove operation logs', 'ENABLED', 121, 0, 0, 0,
        CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'monitor:loginlog:list', 'Login Log List', 'API', 'HTTP_API', '/api/v1/admin/resources/login-logs', 'GET',
        'List login logs', 'ENABLED', 122, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'monitor:loginlog:remove', 'Login Log Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/login-logs/{id}',
        'DELETE', 'Remove login logs', 'ENABLED', 123, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'monitor:online:list', 'Online Session List', 'API', 'HTTP_API', '/api/v1/admin/resources/online-sessions',
        'GET', 'List online sessions', 'ENABLED', 124, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'monitor:online:remove', 'Online Session Remove', 'API', 'HTTP_API',
        '/api/v1/admin/resources/online-sessions/{id}', 'DELETE', 'Remove online sessions', 'ENABLED', 125, 0, 0, 0,
        CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'monitor:server:list', 'Server Monitor View', 'API', 'HTTP_API', '/api/v1/monitor/server', 'GET',
        'View server monitor', 'ENABLED', 126, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'monitor:cache:list', 'Cache Monitor View', 'API', 'HTTP_API', '/api/v1/monitor/cache', 'GET',
        'View cache monitor', 'ENABLED', 127, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'monitor:druid:list', 'Datasource Monitor View', 'API', 'HTTP_API', '/api/v1/monitor/druid', 'GET',
        'View datasource monitor', 'ENABLED', 128, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'structure:definition:list', 'Structure Definition List', 'API', 'HTTP_API', '/api/v1/structure-definitions',
        'GET', 'List structure definitions', 'ENABLED', 130, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'structure:definition:edit', 'Structure Definition Edit', 'API', 'HTTP_API', '/api/v1/structure-definitions',
        'POST', 'Edit structure definitions', 'ENABLED', 131, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'structure:definition:publish', 'Structure Definition Publish', 'API', 'HTTP_API',
        '/api/v1/structure-definitions/{id}/active-version', 'PUT', 'Publish structure definitions', 'ENABLED', 132, 0,
        0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'flow:studio:list', 'Flow Studio View', 'API', 'HTTP_API', '/api/v1/flows', 'GET', 'View flow studio',
        'ENABLED', 140, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'flow:definition:edit', 'Flow Definition Edit', 'API', 'HTTP_API', '/api/v1/flows', 'POST',
        'Edit flow definitions', 'ENABLED', 141, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'flow:definition:publish', 'Flow Definition Publish', 'API', 'HTTP_API', '/api/v1/flows/{id}/publish',
        'POST', 'Publish flow definitions', 'ENABLED', 142, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'flow:component:edit', 'Flow Component Edit', 'API', 'HTTP_API', '/api/v1/flow/components', 'POST',
        'Edit flow components', 'ENABLED', 143, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:workflow:list', 'Workflow List', 'API', 'HTTP_API', '/api/v1/workflow/definitions', 'GET',
        'List workflow definitions', 'ENABLED', 150, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:workflow:edit', 'Workflow Edit', 'API', 'HTTP_API', '/api/v1/workflow/definitions', 'POST',
        'Edit workflow definitions', 'ENABLED', 151, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:workflow:deploy', 'Workflow Deploy', 'API', 'HTTP_API', '/api/v1/workflow/definitions/{id}/deploy',
        'PUT', 'Deploy workflows', 'ENABLED', 152, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:workflow:start', 'Workflow Start', 'API', 'HTTP_API', '/api/v1/workflow/definitions/{id}/instances',
        'POST', 'Start workflows', 'ENABLED', 153, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:workflow:todo', 'Workflow Todo', 'API', 'HTTP_API', '/api/v1/workflow/tasks/todo', 'GET',
        'View workflow todo tasks', 'ENABLED', 154, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:workflow:approve', 'Workflow Approve', 'API', 'HTTP_API', '/api/v1/workflow/tasks/{id}/approve', 'POST',
        'Approve workflow tasks', 'ENABLED', 155, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:pay:list', 'Payment List', 'API', 'HTTP_API', '/api/v1/pay/orders', 'GET', 'List payment data',
        'ENABLED', 160, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:pay:edit', 'Payment Edit', 'API', 'HTTP_API', '/api/v1/pay/channels', 'POST', 'Edit payment channels',
        'ENABLED', 161, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:pay:order', 'Payment Order', 'API', 'HTTP_API', '/api/v1/pay/orders', 'POST', 'Create payment orders',
        'ENABLED', 162, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:pay:notify', 'Payment Notify', 'API', 'HTTP_API', '/api/v1/pay/orders/{orderNo}/notify', 'POST',
        'Handle payment notify', 'ENABLED', 163, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:mp:list', 'Official Account List', 'API', 'HTTP_API', '/api/v1/mp/accounts', 'GET',
        'List official account data', 'ENABLED', 170, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:mp:edit', 'Official Account Edit', 'API', 'HTTP_API', '/api/v1/mp/accounts', 'POST',
        'Edit official account data', 'ENABLED', 171, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:mp:sync', 'Official Account Sync', 'API', 'HTTP_API', '/api/v1/mp/accounts/{accountId}/menus/sync',
        'POST', 'Sync official account menus', 'ENABLED', 172, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:report:list', 'Report List', 'API', 'HTTP_API', '/api/v1/report/datasets', 'GET', 'List reports',
        'ENABLED', 180, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:report:edit', 'Report Edit', 'API', 'HTTP_API', '/api/v1/report/datasets', 'POST', 'Edit reports',
        'ENABLED', 181, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:report:query', 'Report Query', 'API', 'HTTP_API', '/api/v1/report/datasets/{id}/query', 'POST',
        'Query reports', 'ENABLED', 182, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:report:export', 'Report Export', 'API', 'HTTP_API', '/api/v1/report/datasets/{id}/export.csv', 'POST',
        'Export reports', 'ENABLED', 183, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:mobile:list', 'Mobile Builder List', 'API', 'HTTP_API', '/api/v1/mobile/apps', 'GET',
        'List mobile apps', 'ENABLED', 190, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'biz:mobile:edit', 'Mobile Builder Edit', 'API', 'HTTP_API', '/api/v1/mobile/apps', 'POST',
        'Edit mobile apps', 'ENABLED', 191, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:job:list', 'Job List', 'API', 'HTTP_API', '/api/v1/admin/resources/jobs', 'GET', 'List jobs',
        'ENABLED', 200, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:job:add', 'Job Add', 'API', 'HTTP_API', '/api/v1/admin/resources/jobs', 'POST', 'Create jobs',
        'ENABLED', 201, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:job:edit', 'Job Edit', 'API', 'HTTP_API', '/api/v1/tool/jobs/{id}/pause', 'POST', 'Edit jobs',
        'ENABLED', 202, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:job:remove', 'Job Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/jobs/{id}', 'DELETE',
        'Remove jobs', 'ENABLED', 203, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:job:run', 'Job Run', 'API', 'HTTP_API', '/api/v1/tool/jobs/{id}/run', 'POST', 'Run jobs', 'ENABLED',
        204, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:gen:list', 'Code Generator List', 'API', 'HTTP_API', '/api/v1/tool/gen/tables', 'GET',
        'List generator tables', 'ENABLED', 210, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:gen:add', 'Code Generator Add', 'API', 'HTTP_API', '/api/v1/admin/resources/gen-tables', 'POST',
        'Create generator metadata', 'ENABLED', 211, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:gen:edit', 'Code Generator Edit', 'API', 'HTTP_API', '/api/v1/admin/resources/gen-tables/{id}', 'PUT',
        'Edit generator metadata', 'ENABLED', 212, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:gen:remove', 'Code Generator Remove', 'API', 'HTTP_API', '/api/v1/admin/resources/gen-tables/{id}',
        'DELETE', 'Remove generator metadata', 'ENABLED', 213, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:gen:import', 'Code Generator Import', 'API', 'HTTP_API', '/api/v1/tool/gen/tables/import', 'POST',
        'Import generator tables', 'ENABLED', 214, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
       (1, 'tool:gen:download', 'Code Generator Download', 'API', 'HTTP_API', '/api/v1/tool/gen/tables/{id}/download',
        'GET', 'Download generated code', 'ENABLED', 215, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_menu_permission`
(`tenant_id`, `menu_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT p.`tenant_id`, m.`id`, p.`id`, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP
FROM `sys_permission` p
JOIN `sys_menu` m
  ON m.`tenant_id` = p.`tenant_id`
 AND p.`permission_code` LIKE CONCAT(m.`menu_code`, ':%')
WHERE p.`tenant_id` = 1;

INSERT INTO `sys_role_permission`
(`tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT 1, 1, p.`id`, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP
FROM `sys_permission` p
WHERE p.`tenant_id` = 1;

-- =================================================================
-- V4__enterprise_tooling.sql
-- =================================================================
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

-- =================================================================
-- V5__structure_intelligence.sql
-- =================================================================
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
    KEY         `idx_structure_definition_tenant_status` (`tenant_id`, `status`, `update_time`)
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

-- =================================================================
-- V6__automation_workflow_events.sql
-- =================================================================
CREATE TABLE `hb_node_component`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '节点组件ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `type`          VARCHAR(128) NOT NULL COMMENT '组件类型',
    `version`       VARCHAR(32)  NOT NULL COMMENT '组件版本',
    `name`          VARCHAR(128) NOT NULL COMMENT '组件名称',
    `category`      VARCHAR(64)  NOT NULL COMMENT '组件分类',
    `source`        VARCHAR(32)  NOT NULL COMMENT '组件来源',
    `manifest_json` JSON         NOT NULL COMMENT '组件清单',
    `status`        VARCHAR(16)  NOT NULL COMMENT '状态',
    `sort_no`       INT          NOT NULL COMMENT '排序号',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`     bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hb_node_component_type_version` (`tenant_id`, `type`, `version`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='节点组件';

CREATE TABLE `hb_flow_definition`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
    `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`              VARCHAR(128) NOT NULL COMMENT '流程名称',
    `code`              VARCHAR(128) NOT NULL COMMENT '流程编码',
    `description`       VARCHAR(512) NULL COMMENT '流程说明',
    `status`            VARCHAR(16)  NOT NULL COMMENT '状态',
    `active_version_no` INT NULL COMMENT '当前启用版本号',
    `dsl_json`          JSON NULL COMMENT '流程DSL',
    `create_by`         bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`         bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hb_flow_definition_code` (`tenant_id`, `code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程定义';

CREATE TABLE `hb_flow_version`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程版本ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `flow_id`        BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `version_no`     INT         NOT NULL COMMENT '版本号',
    `dsl_json`       JSON        NOT NULL COMMENT '流程DSL',
    `compile_report` JSON NULL COMMENT '编译报告',
    `status`         VARCHAR(16) NOT NULL COMMENT '状态',
    `published_by`   BIGINT UNSIGNED NULL COMMENT '发布人',
    `published_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发布时间',
    `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`      bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hb_flow_version_no` (`tenant_id`, `flow_id`, `version_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程版本';

CREATE TABLE `hb_connection_credential`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '连接凭据ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`        VARCHAR(128) NOT NULL COMMENT '凭据名称',
    `type`        VARCHAR(32)  NOT NULL COMMENT '凭据类型',
    `config_json` JSON         NOT NULL COMMENT '连接配置',
    `secret_json` JSON NULL COMMENT '敏感配置',
    `status`      VARCHAR(16)  NOT NULL COMMENT '状态',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='连接凭据';

CREATE TABLE `hb_flow_run`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程运行ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `flow_id`        BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `version_no`     INT         NOT NULL COMMENT '版本号',
    `trigger_type`   VARCHAR(64) NOT NULL COMMENT '触发类型',
    `status`         VARCHAR(16) NOT NULL COMMENT '运行状态',
    `input_summary`  JSON NULL COMMENT '输入摘要',
    `output_summary` JSON NULL COMMENT '输出摘要',
    `error_message`  TEXT NULL COMMENT '错误信息',
    `started_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
    `finished_at`    DATETIME(3)     NULL COMMENT '结束时间',
    `elapsed_ms`     BIGINT NULL COMMENT '耗时毫秒',
    `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`      bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_hb_flow_run_status` (`tenant_id`, `flow_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程运行记录';

CREATE TABLE `hb_flow_run_event`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程运行事件ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id`        BIGINT UNSIGNED NOT NULL COMMENT '流程运行ID',
    `node_id`       VARCHAR(128) NOT NULL COMMENT '节点ID',
    `node_type`     VARCHAR(128) NOT NULL COMMENT '节点类型',
    `event_type`    VARCHAR(32)  NOT NULL COMMENT '事件类型',
    `input_json`    JSON NULL COMMENT '输入数据',
    `output_json`   JSON NULL COMMENT '输出数据',
    `error_message` TEXT NULL COMMENT '错误信息',
    `elapsed_ms`    BIGINT NULL COMMENT '耗时毫秒',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`     bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY             `idx_hb_flow_run_event_run` (`tenant_id`, `run_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程运行事件';

CREATE TABLE `flow_wait_state`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程等待状态ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id`          BIGINT UNSIGNED NOT NULL COMMENT '流程运行ID',
    `node_id`         VARCHAR(128) NOT NULL COMMENT '节点ID',
    `correlation_key` VARCHAR(128) NOT NULL COMMENT '关联键',
    `status`          VARCHAR(16)  NOT NULL COMMENT '等待状态',
    `payload_json`    JSON NULL COMMENT '等待载荷',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`       bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_wait_state_correlation` (`tenant_id`, `correlation_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流程等待状态';

CREATE TABLE `wf_process_definition`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审批流程定义ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`           VARCHAR(128) NOT NULL COMMENT '流程名称',
    `definition_key` VARCHAR(128) NOT NULL COMMENT '流程定义键',
    `version_no`     INT          NOT NULL COMMENT '版本号',
    `form_schema`    JSON NULL COMMENT '表单Schema',
    `status`         VARCHAR(16)  NOT NULL COMMENT '状态',
    `deployed_at`    DATETIME(3)     NULL COMMENT '部署时间',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`      bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wf_process_definition_key` (`tenant_id`, `definition_key`, `version_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='审批流程定义';

CREATE TABLE `wf_process_instance`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审批流程实例ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `definition_id`   BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `business_key` VARCHAR(128) NULL COMMENT '业务键',
    `title`        VARCHAR(128) NOT NULL COMMENT '实例标题',
    `initiator_id`    BIGINT UNSIGNED NULL COMMENT '发起人ID',
    `status`       VARCHAR(16)  NOT NULL COMMENT '实例状态',
    `current_task_id` BIGINT UNSIGNED NULL COMMENT '当前任务ID',
    `payload`      JSON NULL COMMENT '业务载荷',
    `started_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
    `ended_at`        DATETIME(3)     NULL COMMENT '结束时间',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`    bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_wf_process_instance_status` (`tenant_id`, `status`, `started_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='审批流程实例';

CREATE TABLE `wf_task`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审批任务ID',
    `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `instance_id`  BIGINT UNSIGNED NOT NULL COMMENT '流程实例ID',
    `name`        VARCHAR(128) NOT NULL COMMENT '任务名称',
    `assignee_id`  BIGINT UNSIGNED NULL COMMENT '处理人ID',
    `status`      VARCHAR(16)  NOT NULL COMMENT '任务状态',
    `comment`     VARCHAR(512) NULL COMMENT '任务备注',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `completed_at` DATETIME(3)     NULL COMMENT '完成时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY           `idx_wf_task_assignee` (`tenant_id`, `assignee_id`, `status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='审批任务';

CREATE TABLE `wf_task_action`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审批任务操作ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `task_id`     BIGINT UNSIGNED NOT NULL COMMENT '审批任务ID',
    `action`      VARCHAR(16) NOT NULL COMMENT '操作类型',
    `operator_id` BIGINT UNSIGNED NULL COMMENT '操作人ID',
    `comment`     VARCHAR(512) NULL COMMENT '操作备注',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY           `idx_wf_task_action_task` (`tenant_id`, `task_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='审批任务操作';

CREATE TABLE `sys_outbox_event`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '出站事件ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `event_id`       VARCHAR(64) NOT NULL COMMENT '事件ID',
    `event_type`     VARCHAR(96) NOT NULL COMMENT '事件类型',
    `aggregate_type` VARCHAR(64) NOT NULL COMMENT '聚合类型',
    `aggregate_id`   VARCHAR(64) NOT NULL COMMENT '聚合ID',
    `payload_json`   JSON        NOT NULL COMMENT '事件载荷',
    `status`         VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT '状态',
    `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `published_at`   DATETIME(3)     NULL COMMENT '发布时间',
    `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`      bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_outbox_event_id` (`event_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='出站事件';

CREATE TABLE `sys_inbox_event`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '入站事件ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `consumer_code` VARCHAR(64) NOT NULL COMMENT '消费者编码',
    `event_id`      VARCHAR(64) NOT NULL COMMENT '事件ID',
    `status`        VARCHAR(16) NOT NULL DEFAULT 'PROCESSED' COMMENT '状态',
    `processed_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '处理时间',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`     bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_inbox_consumer_event` (`consumer_code`, `event_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='入站事件';

-- =================================================================
-- V7__payment.sql
-- =================================================================
CREATE TABLE `pay_channel`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付渠道ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`        VARCHAR(128) NOT NULL COMMENT '渠道名称',
    `provider`    VARCHAR(32)  NOT NULL COMMENT '支付提供方',
    `app_id`      VARCHAR(128) NULL COMMENT '应用ID',
    `app_secret`  VARCHAR(512) NULL COMMENT '应用密钥',
    `status`      VARCHAR(16)  NOT NULL COMMENT '状态',
    `sort_no`     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `config_json` JSON NULL COMMENT '渠道配置',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_pay_channel_provider` (`tenant_id`, `provider`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付渠道';

CREATE TABLE `pay_order`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付订单ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `order_no`    VARCHAR(64)    NOT NULL COMMENT '订单号',
    `channel_id`  BIGINT UNSIGNED NOT NULL COMMENT '支付渠道ID',
    `subject`     VARCHAR(128)   NOT NULL COMMENT '订单标题',
    `amount`      DECIMAL(20, 4) NOT NULL COMMENT '订单金额',
    `currency`    VARCHAR(16)    NOT NULL DEFAULT 'CNY' COMMENT '币种',
    `status`      VARCHAR(16)    NOT NULL COMMENT '支付状态',
    `client_ip`   VARCHAR(64) NULL COMMENT '客户端IP',
    `extra_json`  JSON NULL COMMENT '扩展信息',
    `create_time` datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `paid_at`     DATETIME(3)     NULL COMMENT '支付完成时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_order_no` (`tenant_id`, `order_no`),
    KEY           `idx_pay_order_status` (`tenant_id`, `status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付订单';

CREATE TABLE `pay_transaction`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付流水ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `order_id`       BIGINT UNSIGNED NOT NULL COMMENT '支付订单ID',
    `transaction_no` VARCHAR(64)    NOT NULL COMMENT '支付流水号',
    `provider`       VARCHAR(32)    NOT NULL COMMENT '支付提供方',
    `amount`         DECIMAL(20, 4) NOT NULL COMMENT '交易金额',
    `status`         VARCHAR(16)    NOT NULL COMMENT '交易状态',
    `create_time`    datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`      bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_transaction_no` (`tenant_id`, `transaction_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付流水';

CREATE TABLE `pay_refund`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款单ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `order_id`    BIGINT UNSIGNED NOT NULL COMMENT '支付订单ID',
    `refund_no`   VARCHAR(64)    NOT NULL COMMENT '退款单号',
    `amount`      DECIMAL(20, 4) NOT NULL COMMENT '退款金额',
    `status`      VARCHAR(16)    NOT NULL COMMENT '退款状态',
    `create_time` datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_refund_no` (`tenant_id`, `refund_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付退款';

CREATE TABLE `pay_notify_log`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付通知日志ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `order_id`        BIGINT UNSIGNED NULL COMMENT '支付订单ID',
    `order_no`        VARCHAR(64) NULL COMMENT '订单号',
    `provider`        VARCHAR(32) NOT NULL COMMENT '支付提供方',
    `notify_id`       VARCHAR(96) NOT NULL COMMENT '通知ID',
    `notify_payload`  JSON NULL COMMENT '通知报文',
    `signature_valid` VARCHAR(16) NOT NULL COMMENT '签名校验结果',
    `status`          VARCHAR(16) NOT NULL COMMENT '处理状态',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`       bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_notify_id` (`tenant_id`, `provider`, `notify_id`),
    KEY               `idx_pay_notify_order` (`tenant_id`, `order_no`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付通知日志';

-- =================================================================
-- V8__content_report_mobile.sql
-- =================================================================
CREATE TABLE `mp_account`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公众号账号ID',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`       VARCHAR(128) NULL COMMENT '账号名称',
    `app_id`     VARCHAR(128) NULL COMMENT '公众号AppID',
    `app_secret` VARCHAR(512) NULL COMMENT '公众号AppSecret',
    `token`      VARCHAR(128) NULL COMMENT '消息校验Token',
    `aes_key`    VARCHAR(256) NULL COMMENT '消息加解密密钥',
    `status`     VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`  bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`  bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_mp_account_app` (`tenant_id`, `app_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号账号';

CREATE TABLE `mp_menu`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公众号菜单ID',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `account_id` BIGINT UNSIGNED NULL COMMENT '公众号账号ID',
    `parent_id`  BIGINT UNSIGNED NULL COMMENT '父菜单ID',
    `name`       VARCHAR(128) NULL COMMENT '菜单名称',
    `menu_type`  VARCHAR(32) NULL COMMENT '菜单类型',
    `url`        VARCHAR(512) NULL COMMENT '跳转地址',
    `payload`    JSON NULL COMMENT '菜单载荷',
    `sort_no`    INT NULL COMMENT '排序号',
    `status`     VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`  bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`  bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号菜单';

CREATE TABLE `mp_material`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公众号素材ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `account_id`    BIGINT UNSIGNED NULL COMMENT '公众号账号ID',
    `material_type` VARCHAR(32) NULL COMMENT '素材类型',
    `title`         VARCHAR(128) NULL COMMENT '素材标题',
    `media_id`      VARCHAR(128) NULL COMMENT '媒体ID',
    `url`           VARCHAR(512) NULL COMMENT '素材地址',
    `payload`       JSON NULL COMMENT '素材载荷',
    `status`        VARCHAR(16) NULL COMMENT '状态',
    `create_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`     bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号素材';

CREATE TABLE `mp_auto_reply`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自动回复ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `account_id`    BIGINT UNSIGNED NULL COMMENT '公众号账号ID',
    `keyword`       VARCHAR(128) NULL COMMENT '匹配关键字',
    `match_type`    VARCHAR(16) NULL COMMENT '匹配方式',
    `reply_type`    VARCHAR(32) NULL COMMENT '回复类型',
    `reply_content` TEXT NULL COMMENT '回复内容',
    `sort_no`       INT NULL COMMENT '排序号',
    `status`        VARCHAR(16) NULL COMMENT '状态',
    `create_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`     bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号自动回复';

CREATE TABLE `mp_sync_log`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公众号同步日志ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `account_id`  BIGINT UNSIGNED NULL COMMENT '公众号账号ID',
    `sync_type`   VARCHAR(32) NOT NULL COMMENT '同步类型',
    `status`      VARCHAR(16) NOT NULL COMMENT '同步状态',
    `message`     VARCHAR(512) NULL COMMENT '同步消息',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='公众号同步日志';

CREATE TABLE `report_datasource`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表数据源ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`           VARCHAR(128) NULL COMMENT '数据源名称',
    `datasource_key` VARCHAR(128) NULL COMMENT '数据源标识',
    `config_json`    JSON NULL COMMENT '数据源配置',
    `status`         VARCHAR(16) NULL COMMENT '状态',
    `create_time`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`      bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表数据源';

CREATE TABLE `report_dataset`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表数据集ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`        VARCHAR(128) NULL COMMENT '数据集名称',
    `dataset_key` VARCHAR(128) NULL COMMENT '数据集标识',
    `query_sql`   TEXT NULL COMMENT '查询SQL',
    `params_json` JSON NULL COMMENT '查询参数',
    `status`      VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_report_dataset_key` (`tenant_id`, `dataset_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表数据集';

CREATE TABLE `report_template`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表模板ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `dataset_id`    BIGINT UNSIGNED NULL COMMENT '报表数据集ID',
    `name`          VARCHAR(128) NULL COMMENT '模板名称',
    `template_key`  VARCHAR(128) NULL COMMENT '模板标识',
    `template_json` JSON NULL COMMENT '模板配置',
    `status`        VARCHAR(16) NULL COMMENT '状态',
    `create_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`     bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表模板';

CREATE TABLE `report_query_log`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表查询日志ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `dataset_id`  BIGINT UNSIGNED NULL COMMENT '报表数据集ID',
    `params_json` JSON NULL COMMENT '查询参数',
    `row_count`   INT NULL COMMENT '结果行数',
    `status`      VARCHAR(16) NULL COMMENT '查询状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表查询日志';

CREATE TABLE `report_export_task`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报表导出任务ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `template_id` BIGINT UNSIGNED NULL COMMENT '报表模板ID',
    `status`      VARCHAR(16) NOT NULL COMMENT '导出状态',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='报表导出任务';

CREATE TABLE `mobile_app`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '移动应用ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`        VARCHAR(128) NULL COMMENT '应用名称',
    `app_key`     VARCHAR(128) NULL COMMENT '应用标识',
    `entry_url`   VARCHAR(512) NULL COMMENT '入口地址',
    `status`      VARCHAR(16) NULL COMMENT '状态',
    `config_json` JSON NULL COMMENT '应用配置',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_mobile_app_key` (`tenant_id`, `app_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='移动应用';

CREATE TABLE `mobile_app_version`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '移动应用版本ID',
    `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `app_id`       BIGINT UNSIGNED NOT NULL COMMENT '移动应用ID',
    `version_no`   INT         NOT NULL COMMENT '版本号',
    `schema_json`  JSON NULL COMMENT '应用Schema',
    `status`       VARCHAR(16) NOT NULL COMMENT '状态',
    `published_at` DATETIME(3) NULL COMMENT '发布时间',
    `create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`    bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_mobile_app_version_no` (`tenant_id`, `app_id`, `version_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='移动应用版本';

CREATE TABLE `mobile_page`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '移动页面ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `app_id`      BIGINT UNSIGNED NULL COMMENT '移动应用ID',
    `name`        VARCHAR(128) NULL COMMENT '页面名称',
    `page_key`    VARCHAR(128) NULL COMMENT '页面标识',
    `route_path`  VARCHAR(256) NULL COMMENT '路由路径',
    `schema_json` JSON NULL COMMENT '页面Schema',
    `sort_no`     INT NULL COMMENT '排序号',
    `status`      VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`   bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='移动页面';

CREATE TABLE `mobile_api_route`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '移动接口路由ID',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `app_id`     BIGINT UNSIGNED NULL COMMENT '移动应用ID',
    `name`       VARCHAR(128) NULL COMMENT '路由名称',
    `route_key`  VARCHAR(128) NULL COMMENT '路由标识',
    `method`     VARCHAR(16) NULL COMMENT 'HTTP方法',
    `path`       VARCHAR(256) NULL COMMENT '请求路径',
    `target_url` VARCHAR(512) NULL COMMENT '目标地址',
    `sort_no`    INT NULL COMMENT '排序号',
    `status`     VARCHAR(16) NULL COMMENT '状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`  bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`  bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='移动接口路由';

-- =================================================================
-- V9__flowable_runtime_integration.sql
-- =================================================================
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
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流程触发器ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `flow_id`         BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `flow_version_id` BIGINT UNSIGNED NULL COMMENT '流程版本ID',
    `trigger_code`    VARCHAR(128) NOT NULL COMMENT '触发器编码',
    `trigger_type`    VARCHAR(32)  NOT NULL COMMENT '触发器类型',
    `webhook_key`     VARCHAR(128) NULL COMMENT 'Webhook键',
    `cron_expression` VARCHAR(128) NULL COMMENT 'Cron表达式',
    `event_topic`     VARCHAR(128) NULL COMMENT '领域事件主题',
    `mq_type`         VARCHAR(32) NULL COMMENT '消息队列类型',
    `mq_topic`        VARCHAR(255) NULL COMMENT '消息主题',
    `mq_tag`          VARCHAR(255) NULL COMMENT '消息标签',
    `config_json`     JSON NULL COMMENT '触发器配置',
    `status`          VARCHAR(32)  NOT NULL COMMENT '触发器状态',
    `last_triggered_at` DATETIME(3) NULL COMMENT '最后触发时间',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`       bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_trigger_code` (`tenant_id`, `trigger_code`),
    UNIQUE KEY `uk_flow_webhook_key` (`tenant_id`, `webhook_key`),
    KEY               `idx_flow_trigger_flow` (`tenant_id`, `flow_id`),
    KEY               `idx_flow_trigger_type_status` (`tenant_id`, `trigger_type`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程触发器';

CREATE TABLE `hb_flow_engine_mapping`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '映射ID',
    `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `flow_id`           BIGINT UNSIGNED NOT NULL COMMENT '流程定义ID',
    `flow_version_id`   BIGINT UNSIGNED NOT NULL COMMENT '流程版本ID',
    `flow_node_id`      VARCHAR(128) NOT NULL COMMENT 'Flow节点ID',
    `bpmn_element_id`   VARCHAR(128) NOT NULL COMMENT 'BPMN元素ID',
    `component_type`    VARCHAR(128) NOT NULL COMMENT '组件类型',
    `component_version` VARCHAR(32)  NOT NULL COMMENT '组件版本',
    `executor_id`       VARCHAR(128) NOT NULL COMMENT '执行器ID',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`         bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_node_mapping` (`tenant_id`, `flow_version_id`, `flow_node_id`),
    KEY                 `idx_flow_bpmn_mapping` (`tenant_id`, `flow_version_id`, `bpmn_element_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Flow与BPMN元素映射';

CREATE TABLE `hb_flow_wait_subscription`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '等待订阅ID',
    `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id`           BIGINT UNSIGNED NOT NULL COMMENT '运行ID',
    `engine_instance_id` VARCHAR(128) NOT NULL COMMENT '引擎流程实例ID',
    `execution_id`     VARCHAR(128) NOT NULL COMMENT '引擎执行ID',
    `node_id`          VARCHAR(128) NOT NULL COMMENT '等待节点ID',
    `wait_instance_id` VARCHAR(128) NOT NULL COMMENT '等待实例ID',
    `message_name`     VARCHAR(128) NULL COMMENT '消息名称',
    `correlation_key`  VARCHAR(128) NULL COMMENT '关联键',
    `status`           VARCHAR(32)  NOT NULL COMMENT '等待状态',
    `expire_at`        DATETIME(3) NULL COMMENT '过期时间',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`        bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_wait_instance` (`tenant_id`, `wait_instance_id`),
    KEY                `idx_flow_wait_correlation` (`tenant_id`, `message_name`, `correlation_key`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程等待订阅';

CREATE TABLE `hb_flow_io_command`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '外部IO命令ID',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id`        BIGINT UNSIGNED NOT NULL COMMENT '运行ID',
    `node_id`       VARCHAR(128) NOT NULL COMMENT '节点ID',
    `command_type`  VARCHAR(64)  NOT NULL COMMENT '命令类型',
    `idempotency_key` VARCHAR(128) NOT NULL COMMENT '外部幂等键',
    `request_json`  JSON NULL COMMENT '请求摘要',
    `response_json` JSON NULL COMMENT '响应摘要',
    `status`        VARCHAR(32)  NOT NULL COMMENT '命令状态',
    `attempt_no`    INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '尝试次数',
    `next_attempt_at` DATETIME(3) NULL COMMENT '下次尝试时间',
    `lease_owner`   VARCHAR(128) NULL COMMENT '租约持有人',
    `lease_until`   DATETIME(3) NULL COMMENT '租约截止时间',
    `error_code`    VARCHAR(64) NULL COMMENT '错误编码',
    `error_message` TEXT NULL COMMENT '错误信息',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`     bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_io_idempotency` (`tenant_id`, `idempotency_key`),
    KEY             `idx_flow_io_ready` (`tenant_id`, `status`, `next_attempt_at`),
    KEY             `idx_flow_io_run` (`tenant_id`, `run_id`, `node_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程外部IO命令';

CREATE TABLE `hb_flow_payload`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Payload ID',
    `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `run_id`       BIGINT UNSIGNED NULL COMMENT '运行ID',
    `payload_sha256` CHAR(64) NOT NULL COMMENT 'Payload摘要',
    `payload_json` JSON     NOT NULL COMMENT '脱敏后的Payload内容',
    `create_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by`    bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY            `idx_flow_payload_run` (`tenant_id`, `run_id`),
    KEY            `idx_flow_payload_hash` (`tenant_id`, `payload_sha256`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程Payload瘦身存储';

-- =================================================================
-- V10__flow_operations_ledger.sql
-- =================================================================
ALTER TABLE `hb_flow_run`
    ADD COLUMN `last_event_seq` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '运行内最后事件序号' AFTER `retry_reason`;

UPDATE `hb_flow_run_event`
SET `event_seq` = NULL;
UPDATE `hb_flow_run_event`
SET `event_seq` = `id`;

ALTER TABLE `hb_flow_run_event`
    MODIFY COLUMN `event_seq` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '运行内事件序号';

UPDATE `hb_flow_run` r
    LEFT JOIN (
    SELECT `tenant_id`, `run_id`, MAX (`event_seq`) AS `max_event_seq`
    FROM `hb_flow_run_event`
    GROUP BY `tenant_id`, `run_id`
    ) e
ON e.`tenant_id` = r.`tenant_id`
    AND e.`run_id` = r.`id`
    SET r.`last_event_seq` = COALESCE (e.`max_event_seq`, 0);

CREATE UNIQUE INDEX `uk_flow_run_engine_instance`
    ON `hb_flow_run` (`tenant_id`, `engine_instance_id`);
CREATE INDEX `idx_flow_run_started`
    ON `hb_flow_run` (`tenant_id`, `started_at`, `id`);
CREATE INDEX `idx_flow_run_status_started`
    ON `hb_flow_run` (`tenant_id`, `status`, `started_at`, `id`);
CREATE INDEX `idx_flow_run_flow_started`
    ON `hb_flow_run` (`tenant_id`, `flow_id`, `started_at`, `id`);

-- =================================================================
-- V11__flow_external_io_reliability.sql
-- =================================================================
ALTER TABLE `hb_flow_io_command`
    ADD COLUMN `flow_version_id` BIGINT UNSIGNED NULL COMMENT '流程版本ID' AFTER `run_id`,
    ADD COLUMN `node_type` VARCHAR(128) NULL COMMENT '节点类型快照' AFTER `node_id`,
    ADD COLUMN `node_version` VARCHAR(32) NULL COMMENT '节点版本快照' AFTER `node_type`,
    ADD COLUMN `executor_id` VARCHAR(128) NULL COMMENT '执行器快照' AFTER `node_version`,
    ADD COLUMN `node_config_json` JSON NULL COMMENT '节点配置快照' AFTER `executor_id`,
    ADD COLUMN `engine_instance_id` VARCHAR(128) NULL COMMENT 'Flowable流程实例ID' AFTER `node_config_json`,
    ADD COLUMN `execution_id` VARCHAR(128) NULL COMMENT 'Flowable等待执行ID' AFTER `engine_instance_id`,
    ADD COLUMN `wait_instance_id` VARCHAR(128) NULL COMMENT '等待实例ID' AFTER `execution_id`,
    ADD COLUMN `worker_topic` VARCHAR(128) NOT NULL DEFAULT 'flow-io-generic' COMMENT 'Worker主题' AFTER `command_type`,
    ADD COLUMN `message_name` VARCHAR(128) NOT NULL DEFAULT 'io.node.completed' COMMENT '恢复消息名' AFTER `worker_topic`,
    ADD COLUMN `correlation_key` VARCHAR(128) NULL COMMENT '恢复关联键' AFTER `message_name`,
    ADD COLUMN `max_attempts` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '最大尝试次数' AFTER `attempt_no`,
    ADD COLUMN `lease_token` VARCHAR(64) NULL COMMENT '租约fencing token' AFTER `lease_until`,
    ADD COLUMN `lease_version` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租约版本' AFTER `lease_token`,
    ADD COLUMN `external_call_policy` VARCHAR(32) NOT NULL DEFAULT 'MANUAL_ONLY' COMMENT '外部调用策略' AFTER `lease_version`,
    ADD COLUMN `timeout_at` DATETIME(3) NULL COMMENT '命令截止时间' AFTER `external_call_policy`,
    ADD COLUMN `call_started_at` DATETIME(3) NULL COMMENT '外部调用开始时间' AFTER `timeout_at`,
    ADD COLUMN `completed_at` DATETIME(3) NULL COMMENT '最终完成时间' AFTER `call_started_at`,
    ADD COLUMN `result_applied_at` DATETIME(3) NULL COMMENT '结果应用时间' AFTER `completed_at`;

UPDATE `hb_flow_io_command`
SET `node_type`   = `command_type`,
    `executor_id` = `command_type`
WHERE `node_type` IS NULL
   OR `executor_id` IS NULL;

CREATE UNIQUE INDEX `uk_flow_io_correlation`
    ON `hb_flow_io_command` (`tenant_id`, `correlation_key`);
CREATE INDEX `idx_flow_io_worker_ready`
    ON `hb_flow_io_command` (`tenant_id`, `worker_topic`, `status`, `next_attempt_at`, `id`);
CREATE INDEX `idx_flow_io_lease`
    ON `hb_flow_io_command` (`status`, `lease_until`, `id`);
CREATE INDEX `idx_flow_io_execution`
    ON `hb_flow_io_command` (`tenant_id`, `engine_instance_id`, `execution_id`);

INSERT INTO `sys_job`
(`tenant_id`, `job_code`, `job_name`, `job_group`, `invoke_target`, `cron_expression`,
 `misfire_policy`, `concurrent`, `status`, `version`, `delete_marker`,
 `create_time`, `update_time`, `create_by`, `update_by`)
SELECT 1,
       'flow-io-reconcile',
       'Flow External I/O Lease Reconcile',
       'FLOW',
       'flowExternalIoReconcileJob.reconcileOnce',
       '0/30 * * * * ?',
       'SMART',
       0,
       'ENABLED',
       0,
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0,
       0 WHERE NOT EXISTS (
    SELECT 1 FROM `sys_job`
    WHERE `tenant_id` = 1 AND `job_code` = 'flow-io-reconcile' AND `delete_marker` = 0
);

INSERT INTO `sys_permission`
(`tenant_id`, `permission_code`, `permission_name`, `permission_type`, `resource_type`,
 `resource_path`, `http_method`, `description`, `status`, `sort_no`, `version`, `delete_marker`,
 `create_by`, `create_time`, `update_by`, `update_time`)
SELECT 1,
       'flow:worker:execute',
       'Flow External I/O Worker Execute',
       'API',
       'HTTP_API',
       '/api/v1/flow/io-commands/**',
       'POST',
       'Claim and complete external Flow I/O commands',
       'ENABLED',
       144,
       0,
       0,
       0,
       CURRENT_TIMESTAMP,
       0,
       CURRENT_TIMESTAMP WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `tenant_id` = 1 AND `permission_code` = 'flow:worker:execute' AND `delete_marker` = 0
);

INSERT INTO `sys_role_permission`
(`tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT 1, 1, p.`id`, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP
FROM `sys_permission` p
WHERE p.`tenant_id` = 1
  AND p.`permission_code` = 'flow:worker:execute'
  AND p.`delete_marker` = 0
  AND NOT EXISTS (SELECT 1
                  FROM `sys_role_permission` rp
                  WHERE rp.`tenant_id` = 1
                    AND rp.`role_id` = 1
                    AND rp.`permission_id` = p.`id`);

-- =================================================================
-- V12__normalize_sys_user_password_time.sql
-- =================================================================
ALTER TABLE `sys_user`
    CHANGE COLUMN `password_updated_at` `password_update_time` DATETIME(3) NULL COMMENT '密码更新时间';

-- =================================================================
-- V13__social_provider_auto_registration.sql
-- =================================================================
ALTER TABLE `auth_social_provider`
    ADD COLUMN `auto_register` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether successful social login may create a local user' AFTER `enabled`;

-- =================================================================
-- V14__normalize_default_admin_credential.sql
-- =================================================================
UPDATE `sys_user`
SET `password_hash`        = '$2a$10$GktqfH9ULvSZcjVlVXMvreLEinbIQ8enqXRlIjYxHlYnNC6JI5/Pi',
    `password_update_time` = CURRENT_TIMESTAMP(3),
    `update_time`          = CURRENT_TIMESTAMP(3)
WHERE `tenant_id` = 1
  AND `username` = 'admin'
  AND `delete_marker` = 0
  AND `password_hash` = '$2a$10$CwTycUXWue0Thq9StjUM0uJ8.7o6iOJIsv4u4tIKu3sZvK7N5Sx9e';

-- =================================================================
-- V15__normalize_audit_actor_columns.sql
-- =================================================================
-- Align the physical schema with the String audit fields used by the generated MyBatis model.
-- Numeric actor identifiers are converted losslessly, while service/user names remain supported.

ALTER TABLE `auth_client_grant`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `auth_client_redirect_uri`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `auth_oauth_client`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `auth_session`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `auth_social_binding`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `auth_social_provider`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `flow_wait_state`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_connection_credential`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_flow_definition`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_flow_engine_mapping`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_flow_io_command`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_flow_payload`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_flow_run`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_flow_run_event`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_flow_trigger`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_flow_version`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_flow_wait_subscription`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `hb_node_component`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `mobile_api_route`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `mobile_app`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `mobile_app_version`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `mobile_page`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `mp_account`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `mp_auto_reply`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `mp_material`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `mp_menu`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `mp_sync_log`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `pay_channel`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `pay_notify_log`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `pay_order`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `pay_refund`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `pay_transaction`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `report_dataset`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `report_datasource`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `report_export_task`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `report_query_log`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `report_template`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `structure_artifact`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `structure_definition`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `structure_draft`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `structure_publish_audit`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `structure_version`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_config`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_dept`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_dict_item`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_dict_type`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_gen_column`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_gen_table`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_inbox_event`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_job`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_job_log`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_menu`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_menu_permission`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_notice`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_oper_log`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_outbox_event`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_permission`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_plan_feature`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_post`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_role`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_role_dept`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_role_permission`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_tenant`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_tenant_feature`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_tenant_plan`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_user`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_user_post`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_user_preference`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `sys_user_role`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `wf_process_definition`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `wf_process_instance`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `wf_task`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

ALTER TABLE `wf_task_action`
    MODIFY COLUMN `create_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Creator identifier',
    MODIFY COLUMN `update_by` VARCHAR (64) NOT NULL DEFAULT '0' COMMENT 'Updater identifier';

