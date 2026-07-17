package top.kx.heartbeat.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class EnterpriseMySqlUpgradeTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.36")
                    .withDatabaseName("heartbeat_upgrade")
                    .withUsername("heartbeat")
                    .withPassword("heartbeat-test");

    @Test
    void flywayCanUpgradeFromPlatformToolingToLatestBusinessSchema() throws Exception {
        Flyway phaseOne = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration/mysql")
                .target("4")
                .load();
        phaseOne.migrate();

        try (Connection connection = connect()) {
            assertTrue(tableExists(connection, "sys_user"));
            assertEquals("4", currentVersion(connection));
        }

        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration/mysql")
                .target("9")
                .load()
                .migrate();

        try (Connection connection = connect()) {
            assertEquals("9", currentVersion(connection));
            insertLegacyFlowLedger(connection);
        }

        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration/mysql")
                .load()
                .migrate();

        try (Connection connection = connect()) {
            assertTrue(tableExists(connection, "structure_definition"));
            assertTrue(tableExists(connection, "wf_process_definition"));
            assertTrue(tableExists(connection, "pay_order"));
            assertTrue(tableExists(connection, "mobile_app_version"));
            assertEquals("15", currentVersion(connection));
            assertEquals(9101L, queryLong(connection,
                    "select event_seq from hb_flow_run_event where id = 9101"));
            assertEquals(9101L, queryLong(connection,
                    "select last_event_seq from hb_flow_run where id = 9001"));
        }
    }

    private void insertLegacyFlowLedger(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into hb_flow_definition "
                    + "(id, tenant_id, name, code, status, create_by, update_by) "
                    + "values (9001, 1, 'Legacy flow', 'legacy_flow_v10', 'ONLINE', 0, 0)");
            statement.executeUpdate("insert into hb_flow_run "
                    + "(id, tenant_id, flow_id, version_no, trigger_type, status, create_by, update_by) "
                    + "values (9001, 1, 9001, 1, 'MANUAL', 'RUNNING', 0, 0)");
            statement.executeUpdate("insert into hb_flow_run_event "
                    + "(id, tenant_id, run_id, node_id, node_type, event_type, create_by, update_by) "
                    + "values (9101, 1, 9001, 'node_1', 'HTTP', 'ACTIVITY_COMPLETED', 0, 0)");
        }
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private String currentVersion(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select version from flyway_schema_history where success = 1 order by installed_rank desc limit 1")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getString(1);
            }
        }
    }

    private long queryLong(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next());
            return resultSet.getLong(1);
        }
    }
}
