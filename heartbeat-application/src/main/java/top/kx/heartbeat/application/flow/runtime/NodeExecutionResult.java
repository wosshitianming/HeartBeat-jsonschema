package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行结果。
 *
 * <p>用于承接单个节点执行后的状态、输出、后续端口和错误信息。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionResult {

    /**
     * 节点执行状态。
     */
    private String status;

    /**
     * 节点输出数据。
     */
    private Map<String, Object> output = new LinkedHashMap<>();

    /**
     * 后续端口标识列表。
     */
    private List<String> nextPorts = new ArrayList<>();

    /**
     * 节点执行错误信息。
     */
    private String errorMessage;
}
