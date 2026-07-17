package top.kx.heartbeat.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
            "hb_flow_trigger", "hb_flow_engine_mapping", "hb_flow_wait_subscription", "hb_flow_io_command", "hb_flow_payload",
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
            assertTrue(columnExists(connection, "auth_social_provider", "auto_register"));
            assertTrue(columnExists(connection, "sys_user", "password_update_time"));
            assertTrue(columnIsType(connection, "sys_user", "password_update_time", "datetime"));
            assertTrue(requiredColumnHasDefault(connection, "auth_social_provider", "auto_register", "tinyint", "0"));
            assertTrue(requiredColumnHasDefault(connection, "hb_flow_run", "last_event_seq", "bigint", "0"));
            assertTrue(uniqueIndexExists(connection, "hb_flow_run", "uk_flow_run_engine_instance"));
            assertTrue(uniqueIndexExists(connection, "hb_flow_io_command", "uk_flow_io_correlation"));
            assertTrue(indexExists(connection, "hb_flow_io_command", "idx_flow_io_worker_ready"));
            assertEquals("$2a$10$GktqfH9ULvSZcjVlVXMvreLEinbIQ8enqXRlIjYxHlYnNC6JI5/Pi",
                    queryString(connection, "select password_hash from sys_user where tenant_id = 1 and username = 'admin'"));
            assertAuditDefaultsSupportSelectiveInsert(connection);
            assertTrue(columnExists(connection, "sys_oper_log", "duration_ms"));
            assertFalse(tableExists(connection, "sys_resource_base"));

            for (String table : ENTERPRISE_TABLES) {
                assertTrue(idIsUnsignedBigintAutoIncrement(connection, table),
                        table + ".id should be bigint unsigned auto_increment");
                if ("sys_login_log".equals(table)) {
                    assertBigintAuditColumns(connection, table);
                } else {
                    assertAuditColumns(connection, table);
                }
            }
        }
    }

    private void assertAuditColumns(Connection connection, String tableName) throws SQLException {
        assertTrue(columnExists(connection, tableName, "create_by"),
                tableName + ".create_by should exist");
        assertTrue(columnExists(connection, tableName, "create_time"),
                tableName + ".create_time should exist");
        assertTrue(columnExists(connection, tableName, "update_by"),
                tableName + ".update_by should exist");
        assertTrue(columnExists(connection, tableName, "update_time"),
                tableName + ".update_time should exist");
        assertTrue(columnIsVarchar64WithDefault(connection, tableName, "create_by"),
                tableName + ".create_by should be varchar(64) not null default '0'");
        assertTrue(columnIsVarchar64WithDefault(connection, tableName, "update_by"),
                tableName + ".update_by should be varchar(64) not null default '0'");
    }

    private void assertBigintAuditColumns(Connection connection, String tableName) throws SQLException {
        assertTrue(columnIsUnsignedBigint(connection, tableName, "create_by"),
                tableName + ".create_by should remain bigint unsigned for the Long DO field");
        assertTrue(columnIsUnsignedBigint(connection, tableName, "update_by"),
                tableName + ".update_by should remain bigint unsigned for the Long DO field");
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

    private boolean columnIsVarchar64WithDefault(Connection connection, String tableName, String columnName)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select data_type, character_maximum_length, is_nullable, column_default "
                        + "from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        && "varchar".equalsIgnoreCase(resultSet.getString("data_type"))
                        && resultSet.getLong("character_maximum_length") == 64L
                        && "NO".equalsIgnoreCase(resultSet.getString("is_nullable"))
                        && "0".equals(resultSet.getString("column_default"));
            }
        }
    }

    private boolean columnIsUnsignedBigint(Connection connection, String tableName, String columnName)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select data_type, column_type from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        && "bigint".equalsIgnoreCase(resultSet.getString("data_type"))
                        && resultSet.getString("column_type").toLowerCase().contains("unsigned");
            }
        }
    }

    private boolean columnIsType(Connection connection, String tableName, String columnName,
                                 String dataType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select data_type from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && dataType.equalsIgnoreCase(resultSet.getString("data_type"));
            }
        }
    }

    private boolean requiredColumnHasDefault(Connection connection, String tableName, String columnName,
                                             String dataType, String defaultValue) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select data_type, is_nullable, column_default from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        && dataType.equalsIgnoreCase(resultSet.getString("data_type"))
                        && "NO".equalsIgnoreCase(resultSet.getString("is_nullable"))
                        && defaultValue.equals(resultSet.getString("column_default"));
            }
        }
    }

    private boolean uniqueIndexExists(Connection connection, String tableName, String indexName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select count(*) from information_schema.statistics "
                        + "where table_schema = database() and table_name = ? and index_name = ? and non_unique = 0")) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select count(*) from information_schema.statistics "
                        + "where table_schema = database() and table_name = ? and index_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private String queryString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }

    private void assertAuditDefaultsSupportSelectiveInsert(Connection connection) throws SQLException {
        String orderNo = "AUDIT-SMOKE-001";
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into pay_order "
                        + "(tenant_id, order_no, channel_id, subject, amount, currency, status, create_time, update_time) "
                        + "values (1, ?, 0, 'Audit default smoke test', 1.0000, 'CNY', 'PAYING', now(), now())")) {
            statement.setString(1, orderNo);
            assertEquals(1, statement.executeUpdate());
        }
        assertEquals("0", queryString(connection,
                "select create_by from pay_order where tenant_id = 1 and order_no = '" + orderNo + "'"));
        assertEquals("0", queryString(connection,
                "select update_by from pay_order where tenant_id = 1 and order_no = '" + orderNo + "'"));
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
