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
