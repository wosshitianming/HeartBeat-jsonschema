package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行输出。
 *
 * <p>用于生产态 delegate 在写回变量和选择后续端口时承接执行结果。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionOutcome {

    /**
     * 节点执行状态。
     */
    private String status;

    /**
     * 节点输出载荷。
     */
    private Map<String, Object> output = new LinkedHashMap<>();

    /**
     * 命中的后续端口。
     */
    private List<String> nextPorts = new ArrayList<>();

    /**
     * 错误编码。
     */
    private String errorCode;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 根据节点执行结果创建输出。
     *
     * @param result 节点执行结果
     * @return 节点执行输出
     */
    public static NodeExecutionOutcome from(NodeExecutionResult result) {
        // 创建节点执行输出。
        NodeExecutionOutcome outcome = new NodeExecutionOutcome();
        // 写入执行状态。
        outcome.setStatus(result.getStatus());
        // 写入输出载荷。
        outcome.setOutput(result.getOutput());
        // 写入后续端口。
        outcome.setNextPorts(result.getNextPorts());
        // 写入错误信息。
        outcome.setErrorMessage(result.getErrorMessage());
        // 返回节点执行输出。
        return outcome;
    }
}
