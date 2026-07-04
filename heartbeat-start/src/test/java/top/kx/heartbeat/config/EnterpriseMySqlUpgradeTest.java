package top.kx.heartbeat.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
    void flywayCanUpgradeFromPlatformToolingToV8BusinessSchema() throws Exception {
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
                .load()
                .migrate();

        try (Connection connection = connect()) {
            assertTrue(tableExists(connection, "structure_definition"));
            assertTrue(tableExists(connection, "wf_process_definition"));
            assertTrue(tableExists(connection, "pay_order"));
            assertTrue(tableExists(connection, "mobile_app_version"));
            assertEquals("8", currentVersion(connection));
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
}
