package top.kx.heartbeat.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class EnterpriseMySqlMigrationTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.36")
                    .withDatabaseName("heartbeat")
                    .withUsername("heartbeat")
                    .withPassword("heartbeat-test");

    private static final List<String> ENTERPRISE_TABLES = Arrays.asList(
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
            "sys_login_log",
            "sys_gen_table",
            "sys_gen_column",
            "sys_job",
            "sys_job_log",
            "structure_definition", "structure_draft", "structure_version", "structure_artifact", "structure_publish_audit",
            "hb_node_component", "hb_flow_definition", "hb_flow_version", "hb_connection_credential", "hb_flow_run", "hb_flow_run_event",
            "flow_wait_state", "wf_process_definition", "wf_process_instance", "wf_task", "wf_task_action",
            "sys_outbox_event", "sys_inbox_event",
            "pay_channel", "pay_order", "pay_transaction", "pay_refund", "pay_notify_log",
            "mp_account", "mp_menu", "mp_material", "mp_auto_reply", "mp_sync_log",
            "report_datasource", "report_dataset", "report_template", "report_query_log", "report_export_task",
            "mobile_app", "mobile_app_version", "mobile_page", "mobile_api_route"
    );

    @Test
    void flywayMigrationsCreateEnterpriseSchemaOnMysql8() throws Exception {
        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration/mysql")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            assertTrue(columnExists(connection, "sys_dept", "dept_code"));
            assertTrue(columnExists(connection, "sys_role", "data_scope"));
            assertTrue(columnExists(connection, "sys_config", "config_value"));
            assertTrue(columnExists(connection, "auth_session", "refresh_token_hash"));
            assertTrue(columnExists(connection, "sys_oper_log", "duration_ms"));
            assertFalse(tableExists(connection, "sys_resource_base"));

            for (String table : ENTERPRISE_TABLES) {
                assertTrue(idIsUnsignedBigintAutoIncrement(connection, table),
                        table + ".id should be bigint unsigned auto_increment");
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() and table_name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean idIsUnsignedBigintAutoIncrement(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select data_type, column_type, extra, column_key "
                        + "from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = 'id'")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                String dataType = resultSet.getString("data_type");
                String columnType = resultSet.getString("column_type");
                String extra = resultSet.getString("extra");
                String columnKey = resultSet.getString("column_key");
                return "bigint".equalsIgnoreCase(dataType)
                        && columnType != null
                        && columnType.toLowerCase().contains("unsigned")
                        && extra != null
                        && extra.toLowerCase().contains("auto_increment")
                        && "PRI".equalsIgnoreCase(columnKey);
            }
        }
    }
}
