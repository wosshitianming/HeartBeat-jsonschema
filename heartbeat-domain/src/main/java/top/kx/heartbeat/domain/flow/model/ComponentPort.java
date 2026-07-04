package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点组件端口模型。
 *
 * <p>用于描述节点组件输入端口或输出端口的展示与结构信息。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPort {

    /**
     * 端口标识。
     */
    private String id;

    /**
     * 端口展示名称。
     */
    private String label;

    /**
     * 端口数据结构。
     */
    private String schema;

    /**
     * 是否必填端口。
     */
    private boolean required;
}
