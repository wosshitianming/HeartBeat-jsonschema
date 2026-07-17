package top.kx.heartbeat.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseSqlOrganizationTest {

    private static final String ENTERPRISE_AGGREGATE = "db/mysql/heartbeat-enterprise-all.sql";
    private static final List<String> FLYWAY_VERSIONS = Arrays.asList(
            "V1__enterprise_platform_iam_auth.sql",
            "V2__platform_seed.sql",
            "V3__permission_seed.sql",
            "V4__enterprise_tooling.sql",
            "V5__structure_intelligence.sql",
            "V6__automation_workflow_events.sql",
            "V7__payment.sql",
            "V8__content_report_mobile.sql",
            "V9__flowable_runtime_integration.sql",
            "V10__flow_operations_ledger.sql",
            "V11__flow_external_io_reliability.sql",
            "V12__normalize_sys_user_password_time.sql",
            "V13__social_provider_auto_registration.sql",
            "V14__normalize_default_admin_credential.sql",
            "V15__normalize_audit_actor_columns.sql",
            "V16__quartz_scheduler_tables.sql"
    );

    @Test
    void enterpriseAggregateIsGeneratedFromFlywayMigrationsInOrder() throws IOException {
        String sql = resource(ENTERPRISE_AGGREGATE);
        assertTrue(sql.startsWith("-- Generated from Flyway migrations. Do not edit manually."));

        int previous = -1;
        for (String version : FLYWAY_VERSIONS) {
            int index = sql.indexOf(version);
            assertTrue(index > previous, "Aggregate should contain " + version + " after previous migration");
            previous = index;
        }

        assertEquals(normalizeLineEndings(generatedAggregate()), normalizeLineEndings(sql),
                "Enterprise aggregate must be regenerated from the Flyway migrations without manual schema folding");
    }

    @Test
    void enterpriseAggregateDoesNotContainLegacySqlModels() throws IOException {
        String sql = resource(ENTERPRISE_AGGREGATE);
        String upper = sql.toUpperCase(Locale.ROOT);

        assertFalse(sql.contains("sys_resource_base"));
        assertFalse(java.util.regex.Pattern.compile("(?is)CREATE\\s+TABLE\\s+[^;]*\\s+LIKE\\s+")
                .matcher(upper)
                .find());
        assertFalse(upper.contains("VARCHAR(36) NOT NULL PRIMARY KEY"));
        assertFalse(java.util.regex.Pattern.compile("(?im)^\\s*SOURCE\\s+")
                .matcher(upper)
                .find());
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

    private String generatedAggregate() throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("-- Generated from Flyway migrations. Do not edit manually.\n");
        builder.append("SET NAMES utf8mb4;\n");
        builder.append("SET time_zone = '+00:00';\n\n");
        for (String version : FLYWAY_VERSIONS) {
            builder.append("-- =================================================================\n");
            builder.append("-- ").append(version).append('\n');
            builder.append("-- =================================================================\n");
            builder.append(resource("db/migration/mysql/" + version).trim()).append("\n\n");
        }
        return builder.toString();
    }

    private String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }
}
