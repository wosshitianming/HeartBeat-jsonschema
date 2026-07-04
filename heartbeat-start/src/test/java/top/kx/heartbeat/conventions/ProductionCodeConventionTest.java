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

class ProductionCodeConventionTest {

    private static final Pattern MANUAL_STRING_EMPTY_CHECK = Pattern.compile(
            "\\.trim\\(\\)\\.(?:isEmpty\\(\\)|length\\(\\)\\s*(?:==|!=|>|<|>=|<=)\\s*0)"
                    + "|\\.length\\(\\)\\s*(?:==|!=|>|<|>=|<=)\\s*0"
    );

    private static final List<String> MODULES = Arrays.asList(
            "heartbeat-domain",
            "heartbeat-application",
            "heartbeat-infrastructure",
            "heartbeat-interfaces",
            "heartbeat-start"
    );

    @Test
    void springManagedProductionCodeUsesResourceFieldInjection() throws IOException {
        List<String> violations = new ArrayList<>();
        for (SourceFile sourceFile : productionSources()) {
            if (sourceFile.content.contains("@Autowired")) {
                violations.add(sourceFile.relativePath + " uses @Autowired");
            }
            if (isSpringManaged(sourceFile.content)
                    && sourceFile.content.contains("@RequiredArgsConstructor")) {
                violations.add(sourceFile.relativePath + " uses @RequiredArgsConstructor");
            }
        }

        assertTrue(violations.isEmpty(),
                "Production Spring injection convention violations:\n" + String.join("\n", violations));
    }

    @Test
    void productionCodeUsesCommonsForStringEmptinessChecks() throws IOException {
        List<String> violations = new ArrayList<>();
        for (SourceFile sourceFile : productionSources()) {
            String[] lines = sourceFile.content.split("\\r?\\n");
            for (int index = 0; index < lines.length; index++) {
                if (MANUAL_STRING_EMPTY_CHECK.matcher(lines[index]).find()) {
                    violations.add(sourceFile.relativePath + ":" + (index + 1) + " " + lines[index].trim());
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Use Apache Commons StringUtils for string emptiness checks:\n"
                        + String.join("\n", violations));
    }

    private boolean isSpringManaged(String source) {
        return source.contains("@Component")
                || source.contains("@Service")
                || source.contains("@Repository")
                || source.contains("@Controller")
                || source.contains("@RestController")
                || source.contains("@Configuration");
    }

    private List<SourceFile> productionSources() throws IOException {
        Path projectRoot = findProjectRoot();
        List<SourceFile> sources = new ArrayList<>();
        for (String module : MODULES) {
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

    private static final class SourceFile {
        private final String relativePath;
        private final String content;

        private SourceFile(String relativePath, String content) {
            this.relativePath = relativePath;
            this.content = content;
        }
    }
}
