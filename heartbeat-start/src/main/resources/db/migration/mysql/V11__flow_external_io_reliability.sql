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
