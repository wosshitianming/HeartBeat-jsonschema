package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点组件端口集合模型。
 *
 * <p>用于描述节点组件的输入端口集合和输出端口集合。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPorts {

    /**
     * 输入端口列表。
     */
    private List<ComponentPort> inputs = new ArrayList<>();

    /**
     * 输出端口列表。
     */
    private List<ComponentPort> outputs = new ArrayList<>();
}
