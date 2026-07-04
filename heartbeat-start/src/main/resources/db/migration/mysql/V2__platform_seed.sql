SET NAMES utf8mb4;
SET time_zone = '+00:00';

INSERT INTO `sys_tenant_plan`
(`id`, `plan_code`, `plan_name`, `plan_type`, `description`, `max_user_count`, `max_storage_mb`,
 `feature_policy`, `status`, `sort_no`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 'ENTERPRISE', 'Enterprise Edition', 'ENTERPRISE', 'Default enterprise plan', 1000, 102400,
 JSON_OBJECT('edition', 'enterprise'), 'ENABLED', 1, 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_plan_feature`
(`id`, `plan_id`, `feature_code`, `feature_name`, `feature_group`, `feature_type`, `quota_limit`, `enabled`,
 `config_schema`, `status`, `sort_no`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
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
VALUES
(1, 1, 'STRUCTURE_INTELLIGENCE', 'Structure Intelligence', 1, NULL, 0, CURRENT_TIMESTAMP, NULL,
 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
(2, 1, 'FLOW_AUTOMATION', 'Flow Automation', 1, NULL, 0, CURRENT_TIMESTAMP, NULL,
 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP),
(3, 1, 'IAM_AUDIT', 'IAM And Audit', 1, NULL, 0, CURRENT_TIMESTAMP, NULL,
 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_dept`
(`id`, `tenant_id`, `parent_id`, `dept_code`, `dept_name`, `ancestors`, `dept_level`, `leader_user_id`,
 `phone`, `email`, `sort_no`, `status`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
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
VALUES
(1, 1, 1, 1, 1, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_role`
(`id`, `tenant_id`, `role_code`, `role_name`, `role_type`, `data_scope`, `description`, `sort_no`, `status`,
 `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 1, 'super_admin', 'Super Administrator', 'SYSTEM', 'ALL', 'Built-in platform super administrator role',
 1, 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_user_role`
(`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 1, 1, 1, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);

INSERT INTO `sys_config`
(`id`, `tenant_id`, `config_key`, `config_name`, `config_value`, `value_type`, `encrypted`, `config_group`,
 `description`, `status`, `version`, `delete_marker`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
(1, 1, 'system.name', 'System Name', 'HeartBeat', 'STRING', 0, 'system', 'Default system display name',
 'ENABLED', 0, 0, 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP);
