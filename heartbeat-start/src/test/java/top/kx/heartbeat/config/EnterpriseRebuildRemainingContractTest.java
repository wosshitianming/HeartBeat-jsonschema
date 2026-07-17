package top.kx.heartbeat.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseRebuildRemainingContractTest {

    private static final List<String> AUTO_ID_TABLES = Arrays.asList(
            "structure_definition", "structure_version", "structure_publish_audit",
            "hb_node_component", "hb_flow_definition", "hb_flow_version",
            "hb_connection_credential", "hb_flow_run", "hb_flow_run_event",
            "wf_process_definition", "wf_process_instance", "wf_task", "wf_task_action",
            "pay_channel", "pay_order", "pay_notify_log",
            "mp_account", "mp_menu", "mp_material", "mp_auto_reply",
            "report_dataset", "report_template", "report_query_log",
            "mobile_app", "mobile_page", "mobile_api_route"
    );
    private static final List<String> LEGACY_SQL_RESOURCES = Arrays.asList(
            "heartbeat-start/src/main/resources/db/mysql/01-structure.sql",
            "heartbeat-start/src/main/resources/db/mysql/02-platform.sql",
            "heartbeat-start/src/main/resources/db/mysql/03-flow.sql",
            "heartbeat-start/src/main/resources/db/mysql/04-workflow.sql",
            "heartbeat-start/src/main/resources/db/mysql/05-business.sql",
            "heartbeat-start/src/main/resources/db/mysql/06-tool.sql",
            "heartbeat-start/src/main/resources/db/mysql/07-quartz.sql",
            "heartbeat-start/src/main/resources/db/mysql/90-seed.sql",
            "heartbeat-start/src/main/resources/db/mysql/heartbeat-all.sql",
            "heartbeat-start/src/main/resources/db/heartbeat_sys.sql",
            "heartbeat-start/src/main/resources/db/schema-mysql.sql"
    );

    @Test
    void productionSourceNoLongerContainsRemainingEnterpriseRebuildResidues() throws IOException {
        assertSourceAbsent("SnowflakeIdGenerator");
        assertSourceAbsent("BusinessFlexRepository");
        assertSourceAbsent("BusinessSqlMapper");
        assertSourceAbsent("listResource(String resource)");
        assertSourceAbsent("createResource(String resource");
        assertSourceAbsent("updateResource(String resource");
        assertSourceAbsent("deleteResource(String resource");
    }

    @Test
    void formalSqlNoLongerContainsLegacyResourceBaseOrCloneTables() throws IOException {
        assertSqlAbsent("sys_resource_base");
        assertSqlAbsent("CREATE TABLE LIKE");
    }

    @Test
    void legacyCompatibilitySqlNoLongerContainsHistoricalCreateTableTemplates() throws IOException {
        assertLegacySqlAbsent("CREATE TABLE `");
    }

    @Test
    void remainingBusinessTablesUseAutoIncrementLongPrimaryKeys() throws IOException {
        String combinedSql = readSqlDirectory(
                projectRoot().resolve("heartbeat-start/src/main/resources/db/migration/mysql"));
        List<String> failures = new ArrayList<String>();
        for (String table : AUTO_ID_TABLES) {
            String body = tableBody(combinedSql, table);
            if (body == null) {
                failures.add(table + " is missing from Flyway migrations");
                continue;
            }
            if (!hasAutoIncrementLongId(body)) {
                failures.add(table + " id must be BIGINT auto-increment, but was:\n" + firstIdLine(body));
            }
        }
        assertTrue(failures.isEmpty(), "Business table primary-key contract violations:\n"
                + String.join("\n", failures));
    }

    private void assertSourceAbsent(String needle) throws IOException {
        List<String> matches = javaFilesUnder(
                projectRoot().resolve("heartbeat-domain/src/main/java"),
                projectRoot().resolve("heartbeat-application/src/main/java"),
                projectRoot().resolve("heartbeat-infrastructure/src/main/java"),
                projectRoot().resolve("heartbeat-interfaces/src/main/java"),
                projectRoot().resolve("heartbeat-start/src/main/java")
        ).stream()
                .filter(path -> contains(path, needle))
                .map(projectRoot()::relativize)
                .map(Path::toString)
                .sorted()
                .collect(Collectors.toList());
        assertTrue(matches.isEmpty(), "Production source must not contain '" + needle + "':\n"
                + String.join("\n", matches));
    }

    private void assertSqlAbsent(String needle) throws IOException {
        List<String> matches = sqlFilesUnder(
                projectRoot().resolve("heartbeat-start/src/main/resources/db"),
                projectRoot().resolve("heartbeat-start/src/main/resources")
        ).stream()
                .filter(path -> containsSqlNeedle(path, needle))
                .map(projectRoot()::relativize)
                .map(Path::toString)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        assertTrue(matches.isEmpty(), "Formal SQL resources must not contain '" + needle + "':\n"
                + String.join("\n", matches));
    }

    private void assertLegacySqlAbsent(String needle) throws IOException {
        List<String> matches = LEGACY_SQL_RESOURCES.stream()
                .map(resource -> projectRoot().resolve(resource))
                .filter(Files::exists)
                .filter(path -> containsSqlNeedle(path, needle))
                .map(projectRoot()::relativize)
                .map(Path::toString)
                .sorted()
                .collect(Collectors.toList());
        assertTrue(matches.isEmpty(), "Legacy compatibility SQL must not contain '" + needle + "':\n"
                + String.join("\n", matches));
    }

    private List<Path> javaFilesUnder(Path... roots) throws IOException {
        List<Path> result = new ArrayList<Path>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                result.addAll(stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .collect(Collectors.toList()));
            }
        }
        return result;
    }

    private List<Path> sqlFilesUnder(Path... roots) throws IOException {
        List<Path> result = new ArrayList<Path>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                result.addAll(stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".sql"))
                        .collect(Collectors.toList()));
            }
        }
        return result;
    }

    private boolean contains(Path path, String needle) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).contains(needle);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read " + path, ex);
        }
    }

    private boolean containsSqlNeedle(Path path, String needle) {
        String sql;
        try {
            sql = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read " + path, ex);
        }
        if ("CREATE TABLE LIKE".equals(needle)) {
            return Pattern.compile("(?is)CREATE\\s+TABLE\\s+[^;]*\\s+LIKE\\s+").matcher(sql).find();
        }
        return sql.toUpperCase(Locale.ROOT).contains(needle.toUpperCase(Locale.ROOT));
    }

    private String readSqlDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Path path : sqlFilesUnder(root)) {
            builder.append(readIfExists(path)).append('\n');
        }
        return builder.toString();
    }

    private String readIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return "";
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String tableBody(String sql, String tableName) {
        Pattern pattern = Pattern.compile("(?is)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?"
                + Pattern.quote(tableName) + "`?\\s*\\((.*?)\\)\\s*(?:ENGINE\\s*=|;)");
        Matcher matcher = pattern.matcher(sql);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean hasAutoIncrementLongId(String body) {
        return Pattern.compile("(?is)`?id`?\\s+BIGINT(?:\\s+UNSIGNED)?\\s+NOT\\s+NULL\\s+AUTO_INCREMENT")
                .matcher(body).find()
                || Pattern.compile("(?is)`?id`?\\s+BIGINT\\s+GENERATED\\s+BY\\s+DEFAULT\\s+AS\\s+IDENTITY")
                .matcher(body).find();
    }

    private String firstIdLine(String body) {
        for (String line : body.split("\\R")) {
            if (line.trim().toLowerCase(Locale.ROOT).matches("`?id`?\\s+.*")) {
                return line.trim();
            }
        }
        return "<missing id column>";
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        if (Files.exists(current.resolve("heartbeat-start"))) {
            return current;
        }
        Path parent = current.getParent();
        while (parent != null) {
            if (Files.exists(parent.resolve("heartbeat-start"))) {
                return parent;
            }
            parent = parent.getParent();
        }
        return current;
    }
}
