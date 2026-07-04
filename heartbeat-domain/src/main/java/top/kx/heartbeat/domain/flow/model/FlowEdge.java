package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程连线领域模型。
 *
 * <p>用于描述源节点端口到目标节点端口之间的执行流向。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowEdge {

    /**
     * 连线标识。
     */
    private String id;

    /**
     * 源节点标识。
     */
    private String source;

    /**
     * 源节点端口标识。
     */
    private String sourcePort;

    /**
     * 目标节点标识。
     */
    private String target;

    /**
     * 目标节点端口标识。
     */
    private String targetPort;
}
