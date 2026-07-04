package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程节点领域模型。
 *
 * <p>用于描述流程画布中的单个可执行节点。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowNode {

    /**
     * 节点标识。
     */
    private String id;

    /**
     * 节点组件类型。
     */
    private String type;

    /**
     * 节点组件版本。
     */
    private String version;

    /**
     * 节点画布位置。
     */
    private FlowPosition position;

    /**
     * 节点配置参数。
     */
    private Map<String, Object> config = new LinkedHashMap<>();
}
