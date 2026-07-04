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
