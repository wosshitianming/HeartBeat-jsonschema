package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.domain.flow.model.FlowNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 节点执行上下文。
 *
 * <p>用于在节点执行器之间传递运行标识、节点定义、输入数据和流程变量。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionContext {

    /**
     * 流程运行标识。
     */
    private String runId;

    /**
     * 当前执行节点。
     */
    private FlowNode node;

    /**
     * 当前节点输入数据。
     */
    private Map<String, Object> input = new LinkedHashMap<>();

    /**
     * 流程变量数据。
     */
    private Map<String, Object> variables = new LinkedHashMap<>();
}
