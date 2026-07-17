package top.kx.heartbeat.application.flow.runtime.node;

import org.junit.jupiter.api.Test;
import top.kx.heartbeat.application.flow.runtime.NodeExecutionContext;
import top.kx.heartbeat.application.flow.runtime.NodeExecutionResult;
import top.kx.heartbeat.domain.flow.model.FlowNode;
import top.kx.heartbeat.domain.flow.model.FlowNodeRunStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UppercaseCodeNodeTest {

    private final UppercaseCodeNode nodeExecutor = new UppercaseCodeNode();

    @Test
    void transformsConfiguredTextField() {
        FlowNode node = new FlowNode();
        node.setConfig(Collections.<String, Object>singletonMap("field", "name"));
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("name", "heartbeat flow");

        NodeExecutionResult result = nodeExecutor.execute(
                new NodeExecutionContext("run-1", node, input, Collections.emptyMap()));

        assertEquals(FlowNodeRunStatus.SUCCESS.getCode(), result.getStatus());
        assertEquals("HEARTBEAT FLOW", result.getOutput().get("name"));
        assertEquals(Collections.singletonList("out"), result.getNextPorts());
    }

    @Test
    void routesInvalidInputToErrorPort() {
        FlowNode node = new FlowNode();
        node.setConfig(new LinkedHashMap<>());

        NodeExecutionResult result = nodeExecutor.execute(
                new NodeExecutionContext("run-2", node, Collections.emptyMap(), Collections.emptyMap()));

        assertEquals(FlowNodeRunStatus.FAILED.getCode(), result.getStatus());
        assertEquals(Collections.singletonList("error"), result.getNextPorts());
        assertTrue(result.getErrorMessage().contains("不存在或不是文本"));
    }
}
