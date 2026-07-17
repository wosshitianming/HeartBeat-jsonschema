package top.kx.heartbeat.application.flow.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.kx.heartbeat.application.flow.runtime.node.UppercaseCodeNode;
import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class NodeExecutorRegistryTest {

    @Test
    void discoversExecutorAndManifestFromCodeNode() {
        NodeExecutorRegistry registry = registry(Collections.<NodeExecutor>singletonList(new UppercaseCodeNode()));

        assertTrue(registry.contains("code:transform.text.uppercase"));
        assertEquals(1, registry.codeManifests().size());
        NodeComponentManifest manifest = registry.codeManifests().get(0);
        assertEquals("transform.text.uppercase", manifest.getType());
        assertEquals("code:transform.text.uppercase", manifest.getRuntime().getExecutor());
        assertEquals("CODE", manifest.getSource());
    }

    @Test
    void rejectsDuplicateExecutorIds() {
        NodeExecutor first = executor("duplicate");
        NodeExecutor second = executor("duplicate");

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> registry(Arrays.asList(first, second)));

        assertTrue(error.getMessage().contains("执行器标识重复"));
    }

    @Test
    void rejectsManifestBoundToAnotherExecutor() {
        CodeNode invalid = new CodeNode() {
            @Override
            public NodeComponentManifest manifest() {
                NodeComponentManifest manifest = NodeManifestBuilder
                        .codeNode(this, "transform.invalid", "Invalid")
                        .build();
                manifest.getRuntime().setExecutor("another-executor");
                return manifest;
            }

            @Override
            public String executorId() {
                return "invalid-executor";
            }

            @Override
            public NodeExecutionResult execute(NodeExecutionContext context) {
                return null;
            }
        };

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> registry(Collections.<NodeExecutor>singletonList(invalid)));

        assertTrue(error.getMessage().contains("Manifest 执行器不匹配"));
    }

    private NodeExecutorRegistry registry(java.util.List<NodeExecutor> executors) {
        NodeExecutorRegistry registry = new NodeExecutorRegistry();
        ReflectionTestUtils.setField(registry, "builtins", new BuiltinNodeExecutors());
        ReflectionTestUtils.setField(registry, "springExecutors", executors);
        registry.initialize();
        return registry;
    }

    private NodeExecutor executor(String id) {
        return new NodeExecutor() {
            @Override
            public String executorId() {
                return id;
            }

            @Override
            public NodeExecutionResult execute(NodeExecutionContext context) {
                return null;
            }
        };
    }
}
