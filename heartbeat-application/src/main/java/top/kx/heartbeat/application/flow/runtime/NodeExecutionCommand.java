package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.domain.flow.model.FlowNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 节点执行命令。
 *
 * <p>用于 Flowable delegate 或调试执行器把节点执行请求交给 HeartBeat 节点执行器。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionCommand {

    /**
     * 流程运行标识。
     */
    private String runId;

    /**
     * 当前节点定义。
     */
    private FlowNode node;

    /**
     * 节点输入载荷。
     */
    private Map<String, Object> input = new LinkedHashMap<>();

    /**
     * 流程变量。
     */
    private Map<String, Object> variables = new LinkedHashMap<>();
}
