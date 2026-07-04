package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程定义领域模型。
 *
 * <p>用于描述可视化流程的基础信息、节点、连线、变量和版本状态。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowDefinition {

    /**
     * 流程定义标识。
     */
    private String id;

    /**
     * 流程定义名称。
     */
    private String name;

    /**
     * 流程定义编码。
     */
    private String code;

    /**
     * 流程定义描述。
     */
    private String description;

    /**
     * 流程定义状态。
     */
    private String status;

    /**
     * 当前激活版本号。
     */
    private Integer activeVersionNo;

    /**
     * 生产态运行时引擎。
     */
    private String runtimeEngine;

    /**
     * 当前激活 Flowable 流程定义标识。
     */
    private String activeProcessDefinitionId;

    /**
     * 当前激活 Flowable 部署标识。
     */
    private String activeDeploymentId;

    /**
     * 流程变量集合。
     */
    private Map<String, Object> variables = new LinkedHashMap<>();

    /**
     * 流程节点列表。
     */
    private List<FlowNode> nodes = new ArrayList<>();

    /**
     * 流程连线列表。
     */
    private List<FlowEdge> edges = new ArrayList<>();

    /**
     * 流程扩展设置。
     */
    private Map<String, Object> settings = new LinkedHashMap<>();

    /**
     * 创建时间。
     */
    private Instant createTime;

    /**
     * 更新时间。
     */
    private Instant updateTime;
}
