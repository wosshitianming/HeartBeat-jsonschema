package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 节点组件清单领域模型。
 *
 * <p>用于描述流程节点组件的元数据、端口、配置结构和运行时信息。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeComponentManifest {

    /**
     * 组件标识。
     */
    private String id;

    /**
     * 组件类型。
     */
    private String type;

    /**
     * 组件版本。
     */
    private String version;

    /**
     * 组件名称。
     */
    private String name;

    /**
     * 组件分类。
     */
    private String category;

    /**
     * 组件描述。
     */
    private String description;

    /**
     * 组件图标。
     */
    private String icon;

    /**
     * 组件来源。
     */
    private String source;

    /**
     * 组件端口定义。
     */
    private ComponentPorts ports = new ComponentPorts();

    /**
     * 组件配置 JSON Schema。
     */
    private Map<String, Object> configSchema = new LinkedHashMap<>();

    /**
     * 组件运行时定义。
     */
    private ComponentRuntime runtime = new ComponentRuntime();

    /**
     * 组件状态。
     */
    private String status;

    /**
     * 组件排序号。
     */
    private int sortNo;
}
