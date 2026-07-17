package top.kx.heartbeat.application.flow.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import top.kx.heartbeat.domain.flow.model.*;
import top.kx.heartbeat.domain.flow.validation.FlowDslValidator;
import top.kx.heartbeat.domain.flow.validation.FlowValidationResult;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowBpmnCompilerTest {

    @Mock
    private FlowDslValidator validator;
    @InjectMocks
    private FlowBpmnCompiler compiler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(compiler, "objectMapper", new ObjectMapper());
        when(validator.validate(any(), any())).thenReturn(new FlowValidationResult());
    }

    @Test
    void externalIoNodeWaitsAndFallsBackToAFailedEndWhenNoErrorEdgeExists() {
        FlowNode start = node("start", "trigger.manual");
        FlowNode http = node("http", "action.http.request");
        FlowNode end = node("end", "system.end");
        FlowDefinition flow = new FlowDefinition();
        flow.setCode("external_io_flow");
        flow.setName("External I/O flow");
        flow.setNodes(Arrays.asList(start, http, end));
        flow.setEdges(Arrays.asList(
                new FlowEdge("e1", "start", "out", "http", "in"),
                new FlowEdge("e2", "http", "out", "end", "in")
        ));
        NodeComponentManifest manifest = new NodeComponentManifest();
        manifest.setType(http.getType());
        manifest.setVersion(http.getVersion());
        manifest.setRuntime(new ComponentRuntime(
                "builtin:http.request", Collections.singletonList("io"), Collections.emptyList()));

        FlowBpmnCompileResult result = compiler.compile(flow, Collections.singletonList(manifest));

        assertTrue(result.isValid());
        assertTrue(result.getBpmnXml().contains("node_http__io_wait"));
        assertTrue(result.getBpmnXml().contains("node_http__io_result"));
        assertTrue(result.getBpmnXml().contains("node_http__io_failed"));
        assertTrue(result.getBpmnXml().contains("flowConditionEvaluator.matches(execution, 'node_http__io_failure_fallback', 'error')"));
    }

    private FlowNode node(String id, String type) {
        FlowNode node = new FlowNode();
        node.setId(id);
        node.setType(type);
        node.setVersion("1.0.0");
        return node;
    }
}
