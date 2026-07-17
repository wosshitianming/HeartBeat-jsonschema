package top.kx.heartbeat.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseDatabaseContractTest {

    private static final String PHASE_ONE_MIGRATION =
            "db/migration/mysql/V1__enterprise_platform_iam_auth.sql";
    private static final String PLATFORM_SEED =
            "db/migration/mysql/V2__platform_seed.sql";
    private static final String PERMISSION_SEED =
            "db/migration/mysql/V3__permission_seed.sql";
    private static final String TOOLING_MIGRATION =
            "db/migration/mysql/V4__enterprise_tooling.sql";
    private static final String FLOW_OPERATIONS_MIGRATION =
            "db/migration/mysql/V10__flow_operations_ledger.sql";
    private static final String FLOW_EXTERNAL_IO_MIGRATION =
            "db/migration/mysql/V11__flow_external_io_reliability.sql";
    private static final String AUDIT_ACTOR_MIGRATION =
            "db/migration/mysql/V15__normalize_audit_actor_columns.sql";
    @Test
    void phaseOneMigrationUsesDedicatedEnterpriseTables() throws IOException {
        String sql = resource(PHASE_ONE_MIGRATION);
        String upper = sql.toUpperCase(Locale.ROOT);

        assertFalse(sql.contains("sys_resource_base"));
        assertFalse(upper.contains("CREATE TABLE") && upper.contains(" LIKE "));

        assertTrue(sql.contains("`sys_tenant`"));
        assertTrue(sql.contains("`sys_dept`"));
        assertTrue(sql.contains("`sys_post`"));
        assertTrue(sql.contains("`sys_user`"));
        assertTrue(sql.contains("`sys_user_post`"));
        assertTrue(sql.contains("`sys_role`"));
        assertTrue(sql.contains("`sys_permission`"));
        assertTrue(sql.contains("`sys_menu`"));
        assertTrue(sql.contains("`sys_user_role`"));
        assertTrue(sql.contains("`sys_role_permission`"));
        assertTrue(sql.contains("`sys_menu_permission`"));
        assertTrue(sql.contains("`sys_role_dept`"));
        assertTrue(sql.contains("`sys_dict_type`"));
        assertTrue(sql.contains("`sys_dict_item`"));
        assertTrue(sql.contains("`sys_config`"));
        assertTrue(sql.contains("`sys_notice`"));
        assertTrue(sql.contains("`sys_user_preference`"));
        assertTrue(sql.contains("`auth_oauth_client`"));
        assertTrue(sql.contains("`auth_client_grant`"));
        assertTrue(sql.contains("`auth_client_redirect_uri`"));
        assertTrue(sql.contains("`auth_social_provider`"));
        assertTrue(sql.contains("`auth_social_binding`"));
        assertTrue(sql.contains("`auth_session`"));
        assertTrue(sql.contains("`sys_oper_log`"));
        assertTrue(sql.contains("`sys_login_log`"));
    }

    @Test
    void phaseOneMigrationUsesDedicatedColumnsAndAutoIncrementPrimaryKeys() throws IOException {
        String sql = resource(PHASE_ONE_MIGRATION);

        assertTableContains(sql, "sys_dept", "dept_code", "dept_name", "ancestors", "dept_level", "leader_user_id");
        assertTableContains(sql, "sys_role", "role_code", "role_name", "role_type", "data_scope");
        assertTableContains(sql, "sys_config", "config_key", "config_value", "value_type", "encrypted");
        assertTableContains(sql, "sys_oper_log", "request_path", "request_method", "operator_id", "duration_ms");
        assertTableContains(sql, "auth_oauth_client", "client_id", "client_secret_hash", "access_token_ttl", "refresh_token_ttl");

        assertTrue(sql.matches("(?s).*`id`\\s+BIGINT\\s+UNSIGNED\\s+NOT\\s+NULL\\s+AUTO_INCREMENT.*"));
        assertFalse(sql.matches("(?s).*`id`\\s+VARCHAR\\(.*"));
    }

    @Test
    void authSessionSchemaSupportsServerSideLifecycleChecks() throws IOException {
        String mysql = resource(PHASE_ONE_MIGRATION);

        assertTableContains(mysql, "auth_session",
                "tenant_id", "session_id", "user_id", "refresh_token_hash", "status",
                "expire_at", "refresh_expire_at", "revoked_at", "last_access_at");
        assertTrue(mysql.contains("UNIQUE KEY `uk_auth_session_no` (`tenant_id`, `session_id`)"));
        assertTrue(mysql.contains("KEY `idx_auth_session_refresh` (`tenant_id`, `refresh_expire_at`, `status`)"));
    }

    @Test
    void platformSeedDoesNotExposePlaintextSecrets() throws IOException {
        String seed = resource(PLATFORM_SEED);
        String lower = seed.toLowerCase(Locale.ROOT);

        assertFalse(seed.contains("'admin123'"));
        assertFalse(seed.contains("'123456'"));
        assertTrue(seed.contains("$2"));
        assertFalse(lower.contains("client-secret"));
    }

    @Test
    void permissionSeedCreatesMenusPermissionsAndRelations() throws IOException {
        String seed = resource(PERMISSION_SEED);

        assertTrue(seed.contains("INSERT INTO `sys_menu`"));
        assertTrue(seed.contains("INSERT INTO `sys_permission`"));
        assertTrue(seed.contains("INSERT INTO `sys_menu_permission`"));
        assertTrue(seed.contains("INSERT INTO `sys_role_permission`"));
        assertTrue(seed.contains("dashboard:view"));
        assertTrue(seed.contains("system:user:list"));
        assertTrue(seed.contains("system:user:add"));
        assertTrue(seed.contains("system:user:edit"));
        assertTrue(seed.contains("system:user:remove"));
        assertTrue(seed.contains("system:role:grant"));
        assertTrue(seed.contains("structure:definition:publish"));
        assertFalse(seed.contains("'admin123'"));
        assertFalse(seed.contains("'123456'"));
    }

    @Test
    void toolingMigrationUsesDedicatedAutoIncrementTables() throws IOException {
        String mysql = resource(TOOLING_MIGRATION);
        String upper = mysql.toUpperCase(Locale.ROOT);

        assertFalse(mysql.contains("sys_resource_base"));
        assertFalse(upper.contains("CREATE TABLE") && upper.contains(" LIKE "));
        assertTableContains(mysql, "sys_gen_table",
                "table_name", "class_name", "module_name", "base_package", "resource_key", "options_json");
        assertTableContains(mysql, "sys_gen_column",
                "gen_table_id", "column_name", "data_type", "java_type", "java_field", "primary_key");
        assertTableContains(mysql, "sys_job",
                "job_code", "job_name", "job_group", "invoke_target", "cron_expression", "misfire_policy");
        assertTableContains(mysql, "sys_job_log",
                "job_id", "job_code", "invoke_target", "result_status", "duration_ms", "started_at");

    }

    @Test
    void flowOperationsMigrationBackfillsThePerRunEventCursor() throws IOException {
        String sql = resource(FLOW_OPERATIONS_MIGRATION);

        assertTrue(sql.contains("MAX(`event_seq`) AS `max_event_seq`"));
        assertTrue(sql.contains("MODIFY COLUMN `event_seq` BIGINT UNSIGNED NOT NULL DEFAULT 0"));
        assertTrue(sql.contains("e.`tenant_id` = r.`tenant_id`"));
        assertTrue(sql.contains("e.`run_id` = r.`id`"));
        assertTrue(sql.contains("uk_flow_run_engine_instance"));
        assertTrue(sql.contains("idx_flow_run_status_started"));
    }

    @Test
    void externalIoMigrationAddsWorkerFencingAndReconciliation() throws IOException {
        String sql = resource(FLOW_EXTERNAL_IO_MIGRATION);

        assertTrue(sql.contains("`wait_instance_id`"));
        assertTrue(sql.contains("`lease_token`"));
        assertTrue(sql.contains("`lease_version`"));
        assertTrue(sql.contains("`external_call_policy`"));
        assertTrue(sql.contains("`timeout_at`"));
        assertTrue(sql.contains("uk_flow_io_correlation"));
        assertTrue(sql.contains("flowExternalIoReconcileJob.reconcileOnce"));
        assertTrue(sql.contains("flow:worker:execute"));
    }

    @Test
    void auditActorMigrationCoversEveryEnterpriseTableWithAuditColumns() throws IOException {
        StringBuilder schema = new StringBuilder();
        for (String migration : Arrays.asList(
                PHASE_ONE_MIGRATION,
                TOOLING_MIGRATION,
                "db/migration/mysql/V5__structure_intelligence.sql",
                "db/migration/mysql/V6__automation_workflow_events.sql",
                "db/migration/mysql/V7__payment.sql",
                "db/migration/mysql/V8__content_report_mobile.sql",
                "db/migration/mysql/V9__flowable_runtime_integration.sql")) {
            schema.append(resource(migration)).append('\n');
        }

        String auditMigration = resource(AUDIT_ACTOR_MIGRATION);
        Pattern createTable = Pattern.compile(
                "(?is)CREATE\\s+TABLE\\s+`([^`]+)`\\s*\\((.*?)\\)\\s*ENGINE\\s*=");
        Matcher matcher = createTable.matcher(schema);
        int auditedTableCount = 0;
        while (matcher.find()) {
            String tableName = matcher.group(1);
            String body = matcher.group(2);
            if (!body.contains("`create_by`") || !body.contains("`update_by`")) {
                continue;
            }
            auditedTableCount++;
            if ("sys_login_log".equals(tableName)) {
                assertFalse(auditMigration.contains("ALTER TABLE `sys_login_log`"),
                        "sys_login_log keeps BIGINT audit actors because its generated DO uses Long");
                continue;
            }
            Pattern normalizedTable = Pattern.compile(
                    "(?is)ALTER\\s+TABLE\\s+`" + Pattern.quote(tableName) + "`\\s+"
                            + "MODIFY\\s+COLUMN\\s+`create_by`\\s+VARCHAR\\(64\\)\\s+NOT\\s+NULL\\s+DEFAULT\\s+'0'.*?"
                            + "MODIFY\\s+COLUMN\\s+`update_by`\\s+VARCHAR\\(64\\)\\s+NOT\\s+NULL\\s+DEFAULT\\s+'0'");
            assertTrue(normalizedTable.matcher(auditMigration).find(),
                    "V15 must normalize audit actor columns for " + tableName);
        }
        assertTrue(auditedTableCount > 0, "Expected enterprise schema to contain audited tables");
    }

    private void assertTableContains(String sql, String tableName, String... columns) {
        LinkedHashSet<String> actualColumns = columnNames(sql, tableName);
        for (String column : columns) {
            assertTrue(actualColumns.contains(column.toLowerCase(Locale.ROOT)),
                    "Expected " + tableName + " to contain column " + column);
        }
    }

    private LinkedHashSet<String> columnNames(String sql, String tableName) {
        String body = tableBody(sql, tableName);
        String[] lines = body.split("\\R");
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        for (String line : lines) {
            String normalized = line.trim();
            if (normalized.endsWith(",")) {
                normalized = normalized.substring(0, normalized.length() - 1).trim();
            }
            if (normalized.startsWith("`")) {
                int end = normalized.indexOf('`', 1);
                if (end > 1) {
                    columns.add(normalized.substring(1, end).toLowerCase(Locale.ROOT));
                }
                continue;
            }
            Matcher matcher = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s+").matcher(normalized);
            if (matcher.find()) {
                String token = matcher.group(1).toLowerCase(Locale.ROOT);
                if (!Arrays.asList("primary", "unique", "key", "constraint", "index").contains(token)) {
                    columns.add(token);
                }
            }
        }
        return columns;
    }

    private String tableBody(String sql, String tableName) {
        Pattern mysqlPattern = Pattern.compile(
                "(?is)CREATE\\s+TABLE\\s+`" + Pattern.quote(tableName) + "`\\s*\\((.*?)\\)\\s*ENGINE\\s*="
        );
        Matcher mysqlMatcher = mysqlPattern.matcher(sql);
        if (mysqlMatcher.find()) {
            return mysqlMatcher.group(1);
        }
        Pattern genericPattern = Pattern.compile(
                "(?is)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?"
                        + Pattern.quote(tableName) + "`?\\s*\\((.*?)\\)\\s*;"
        );
        Matcher genericMatcher = genericPattern.matcher(sql);
        assertTrue(genericMatcher.find(), "Missing CREATE TABLE block for " + tableName);
        return genericMatcher.group(1);
    }

    private String resource(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        assertTrue(resource.exists(), "Missing SQL resource: " + resourcePath);
        InputStream input = resource.getInputStream();
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) >= 0) {
                output.write(buffer, 0, length);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            input.close();
        }
    }
}
