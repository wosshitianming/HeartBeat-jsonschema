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
