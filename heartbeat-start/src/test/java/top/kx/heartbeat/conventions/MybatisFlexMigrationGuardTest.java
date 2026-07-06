package top.kx.heartbeat.conventions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MyBatis-Flex 迁移守护测试
 * <p>
 * 确保 MyBatis-Flex 相关 API 和依赖已被彻底移除，防止迁移回退。
 * </p>
 */
class MybatisFlexMigrationGuardTest {

    private static final List<String> PRODUCTION_MODULES = Arrays.asList(
            "heartbeat-domain",
            "heartbeat-application",
            "heartbeat-infrastructure",
            "heartbeat-interfaces",
            "heartbeat-start"
    );

    private static final String[] FORBIDDEN_IMPORTS = {
            "com.mybatis-flex",
            "top.kx.heartbeat.infrastructure.persistence.query.QueryWrapper",
            "top.kx.heartbeat.infrastructure.persistence.mapper.BaseMapper"
    };

    private static final String[] FORBIDDEN_METHODS = {
            "selectListByQuery",
            "selectOneByQuery",
            "selectCountByQuery",
            "updateByQuery",
            "deleteByQuery"
    };

    private static final Pattern FLEX_REPOSITORY_PATTERN = Pattern.compile(
            ".*FlexRepository\\.java$"
    );

    @Test
    void noMybatisFlexDependenciesInDependencyManagement() throws IOException {
        Path root = findProjectRoot();
        Path pomXml = root.resolve("pom.xml");
        String content = new String(Files.readAllBytes(pomXml), StandardCharsets.UTF_8);

        List<String> violations = new ArrayList<>();
        if (content.contains("mybatis-flex-spring-boot-starter")
                || content.contains("mybatis-flex-processor")
                || content.contains("mybatis-flex-codegen")) {
            violations.add("pom.xml still contains mybatis-flex dependency management declarations");
        }

        assertTrue(violations.isEmpty(),
                "MyBatis-Flex dependencies must be removed from parent pom.xml:\n"
                        + String.join("\n", violations));
    }

    @Test
    void noFlexCompatibilityApisInProductionCode() throws IOException {
        List<String> violations = new ArrayList<>();

        for (SourceFile sourceFile : productionSources()) {
            for (String forbiddenImport : FORBIDDEN_IMPORTS) {
                if (sourceFile.content.contains("import " + forbiddenImport)) {
                    violations.add(sourceFile.relativePath + " imports " + forbiddenImport);
                }
            }

            for (String forbiddenMethod : FORBIDDEN_METHODS) {
                if (sourceFile.content.contains(forbiddenMethod)) {
                    violations.add(sourceFile.relativePath + " uses " + forbiddenMethod);
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "MyBatis-Flex compatibility APIs must be removed from production code:\n"
                        + String.join("\n", violations));
    }

    @Test
    void noFlexRepositoryClassesInProductionCode() throws IOException {
        Path root = findProjectRoot();
        List<String> violations = new ArrayList<>();

        for (String module : PRODUCTION_MODULES) {
            Path sourceRoot = root.resolve(module).resolve("src/main/java");
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                paths.filter(path -> FLEX_REPOSITORY_PATTERN.matcher(path.toString()).matches())
                        .forEach(path -> violations.add(root.relativize(path).toString()));
            }
        }

        assertTrue(violations.isEmpty(),
                "*FlexRepository classes must be renamed to MyBatis-oriented names:\n"
                        + String.join("\n", violations));
    }

    @Test
    void noSqlMapSupportOrJdbcTemplateInRepository() throws IOException {
        List<String> violations = new ArrayList<>();

        for (SourceFile sourceFile : repositorySources()) {
            if (sourceFile.content.contains("SqlMapSupport")) {
                violations.add(sourceFile.relativePath + " extends SqlMapSupport");
            }
            if (sourceFile.content.contains("JdbcTemplate")) {
                violations.add(sourceFile.relativePath + " uses JdbcTemplate for business persistence");
            }
        }

        assertTrue(violations.isEmpty(),
                "Repositories must not use SqlMapSupport or JdbcTemplate for business persistence:\n"
                        + String.join("\n", violations));
    }

    @Test
    void noCustomBaseMapperExtendsInMappers() throws IOException {
        Path root = findProjectRoot();
        Path mapperDir = root.resolve("heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper");

        if (!Files.isDirectory(mapperDir)) {
            return;
        }

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(mapperDir)) {
            paths.filter(path -> path.toString().endsWith("Mapper.java"))
                    .filter(path -> !path.toString().contains(".gen."))
                    .forEach(path -> {
                        try {
                            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                            if (content.contains("extends BaseMapper")) {
                                violations.add(root.relativize(path).toString() + " extends BaseMapper");
                            }
                        } catch (IOException ignored) {
                        }
                    });
        }

        assertTrue(violations.isEmpty(),
                "Mappers must not extend custom BaseMapper (use generated DOMapper instead):\n"
                        + String.join("\n", violations));
    }

    @Test
    void allBusinessTablesHaveGeneratedArtifacts() throws IOException {
        Path root = findProjectRoot();
        Path entityRoot = root.resolve("heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity");
        Path mapperRoot = root.resolve("heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper");
        Path xmlRoot = root.resolve("heartbeat-infrastructure/src/main/resources/mapper-xml");

        List<String> missingArtifacts = new ArrayList<>();

        List<String> requiredTables = Arrays.asList(
                "wf_process_definition",
                "wf_process_instance",
                "wf_task",
                "wf_task_action",
                "sys_user",
                "hb_flow_definition",
                "hb_flow_version",
                "hb_flow_run",
                "hb_flow_run_event",
                "hb_node_component",
                "hb_connection_credential"
        );

        for (String tableName : requiredTables) {
            String baseName = toPascalCase(tableName);
            String doClass = baseName + "DO";
            String exampleClass = baseName + "DOExample";
            String mapperClass = baseName + "DOMapper";
            String xmlFile = baseName + "DOMapper.xml";

            if (!existsBelow(entityRoot, doClass + ".java")) {
                missingArtifacts.add(doClass + ".java");
            }
            if (!existsBelow(entityRoot, exampleClass + ".java")) {
                missingArtifacts.add(exampleClass + ".java");
            }
            if (!existsBelow(mapperRoot, mapperClass + ".java")) {
                missingArtifacts.add(mapperClass + ".java");
            }
            if (!existsBelow(xmlRoot, xmlFile)) {
                missingArtifacts.add(xmlFile);
            }
        }

        assertTrue(missingArtifacts.isEmpty(),
                "Missing generated artifacts for business tables:\n"
                        + String.join("\n", missingArtifacts));
    }

    @Test
    void sysUserMapperUsesEnterpriseUserColumns() throws IOException {
        Path root = findProjectRoot();
        Path mapperXml = root.resolve("heartbeat-infrastructure/src/main/resources/mapper-xml/sys/SysUserDOMapper.xml");
        String content = new String(Files.readAllBytes(mapperXml), StandardCharsets.UTF_8);

        for (String expectedColumn : Arrays.asList(
                "id", "tenant_id", "dept_id", "username", "nickname", "real_name", "email", "phone",
                "avatar_url", "password_hash", "password_algo", "password_updated_at", "gender",
                "user_type", "status", "last_login_at", "last_login_ip", "version", "delete_marker",
                "create_by", "create_time", "update_by", "update_time"
        )) {
            assertTrue(content.contains(expectedColumn),
                    "SysUserDOMapper.xml should map enterprise sys_user column " + expectedColumn);
        }

        for (String legacyColumn : Arrays.asList(
                "user_id", "user_name", "nick_name", "phonenumber", "del_flag", "login_date", "pwd_update_date"
        )) {
            assertTrue(!content.contains(legacyColumn),
                    "SysUserDOMapper.xml must not use legacy RuoYi sys_user column " + legacyColumn);
        }
    }

    private List<SourceFile> productionSources() throws IOException {
        Path projectRoot = findProjectRoot();
        List<SourceFile> sources = new ArrayList<>();
        for (String module : PRODUCTION_MODULES) {
            Path sourceRoot = projectRoot.resolve(module).resolve("src/main/java");
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> sources.add(readSource(projectRoot, path)));
            }
        }
        return sources;
    }

    private List<SourceFile> repositorySources() throws IOException {
        Path projectRoot = findProjectRoot();
        Path infraRoot = projectRoot.resolve("heartbeat-infrastructure/src/main/java");
        List<SourceFile> sources = new ArrayList<>();

        if (!Files.isDirectory(infraRoot)) {
            return sources;
        }

        try (Stream<Path> paths = Files.walk(infraRoot)) {
            paths.filter(path -> path.toString().endsWith("Repository.java")
                            || path.toString().endsWith("RepositoryImpl.java"))
                    .forEach(path -> sources.add(readSource(projectRoot, path)));
        }
        return sources;
    }

    private SourceFile readSource(Path projectRoot, Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return new SourceFile(projectRoot.relativize(path).toString(), content);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read source file " + path, ex);
        }
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("heartbeat-start"))
                    && Files.isDirectory(current.resolve("heartbeat-application"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate HeartBeat project root");
    }

    private String toPascalCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean nextUpper = true;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                result.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private boolean existsBelow(Path root, String fileName) throws IOException {
        if (!Files.isDirectory(root)) {
            return false;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.anyMatch(path -> fileName.equals(path.getFileName().toString()));
        }
    }

    private static final class SourceFile {
        private final String relativePath;
        private final String content;

        private SourceFile(String relativePath, String content) {
            this.relativePath = relativePath;
            this.content = content;
        }
    }
}
