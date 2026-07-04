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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            "V8__content_report_mobile.sql"
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
}
