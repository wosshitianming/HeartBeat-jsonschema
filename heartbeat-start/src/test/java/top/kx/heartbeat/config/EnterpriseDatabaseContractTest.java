package top.kx.heartbeat.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
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
    private static final List<String> PHASE_ONE_TABLES = Arrays.asList(
            "sys_tenant_plan",
            "sys_plan_feature",
            "sys_tenant",
            "sys_tenant_feature",
            "sys_dept",
            "sys_post",
            "sys_user",
            "sys_user_post",
            "sys_role",
            "sys_permission",
            "sys_menu",
            "sys_user_role",
            "sys_role_permission",
            "sys_menu_permission",
            "sys_role_dept",
            "sys_dict_type",
            "sys_dict_item",
            "sys_config",
            "sys_notice",
            "sys_user_preference",
            "auth_oauth_client",
            "auth_client_grant",
            "auth_client_redirect_uri",
            "auth_social_provider",
            "auth_social_binding",
            "auth_session",
            "sys_oper_log",
            "sys_login_log"
    );
    private static final List<String> TOOLING_TABLES = Arrays.asList(
            "sys_gen_table", "sys_gen_column", "sys_job", "sys_job_log"
    );

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
        String h2 = resource("schema.sql");

        assertTableContains(mysql, "auth_session",
                "tenant_id", "session_id", "user_id", "refresh_token_hash", "status",
                "expire_at", "refresh_expire_at", "revoked_at", "last_access_at");
        assertTableContains(h2, "auth_session",
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
    void localH2SchemaKeepsPhaseOneTableAndColumnParity() throws IOException {
        String mysql = resource(PHASE_ONE_MIGRATION);
        String h2 = resource("schema.sql");

        for (String table : PHASE_ONE_TABLES) {
            assertTrue(hasTable(h2, table), "schema.sql should contain " + table);
            assertTrue(hasTable(mysql, table), "MySQL migration should contain " + table);
            assertTrue(columnNames(h2, table).equals(columnNames(mysql, table)),
                    "schema.sql columns for " + table + " should match MySQL migration. expected="
                            + columnNames(mysql, table) + " actual=" + columnNames(h2, table));
        }
    }

    @Test
    void toolingMigrationUsesDedicatedAutoIncrementTables() throws IOException {
        String mysql = resource(TOOLING_MIGRATION);
        String h2 = resource("schema.sql");
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

        for (String table : TOOLING_TABLES) {
            assertTrue(columnNames(h2, table).equals(columnNames(mysql, table)),
                    "schema.sql columns for " + table + " should match MySQL migration. expected="
                            + columnNames(mysql, table) + " actual=" + columnNames(h2, table));
        }
    }

    private void assertTableContains(String sql, String tableName, String... columns) {
        LinkedHashSet<String> actualColumns = columnNames(sql, tableName);
        for (String column : columns) {
            assertTrue(actualColumns.contains(column.toLowerCase(Locale.ROOT)),
                    "Expected " + tableName + " to contain column " + column);
        }
    }

    private boolean hasTable(String sql, String tableName) {
        Pattern pattern = Pattern.compile("(?is)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?"
                + Pattern.quote(tableName) + "`?\\s*\\(");
        return pattern.matcher(sql).find();
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
