package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flow DSL 节点与 BPMN 元素映射。
 *
 * <p>用于把 Flowable 运行事件重新投影回 Open Flow Studio 画布节点。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowBpmnElementMapping {

    /**
     * Flow DSL 节点标识。
     */
    private String flowNodeId;

    /**
     * BPMN 元素标识。
     */
    private String bpmnElementId;

    /**
     * 节点组件类型。
     */
    private String componentType;

    /**
     * 节点组件版本。
     */
    private String componentVersion;

    /**
     * 节点执行器标识。
     */
    private String executorId;
}
