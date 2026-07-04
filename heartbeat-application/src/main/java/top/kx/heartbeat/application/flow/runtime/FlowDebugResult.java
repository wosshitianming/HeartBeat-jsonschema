package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程调试结果响应模型。
 *
 * <p>用于承接流程调试后的运行标识、状态、输出和节点事件。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowDebugResult {

    /**
     * 流程运行标识。
     */
    private String runId;

    /**
     * 流程调试状态。
     */
    private String status;

    /**
     * 流程调试输出数据。
     */
    private Map<String, Object> output = new LinkedHashMap<>();

    /**
     * 流程运行事件列表。
     */
    private List<FlowRunEvent> events = new ArrayList<>();
}
