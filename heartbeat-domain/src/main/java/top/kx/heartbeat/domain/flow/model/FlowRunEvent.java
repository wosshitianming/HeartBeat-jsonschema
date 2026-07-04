package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程运行事件领域模型。
 *
 * <p>用于记录单个节点执行产生的输入、输出、状态和耗时。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowRunEvent {

    /**
     * 运行事件标识。
     */
    private String id;

    /**
     * 租户标识。
     */
    private String tenantId;

    /**
     * 流程运行标识。
     */
    private String runId;

    /**
     * 运行内事件序号。
     */
    private Long eventSeq;

    /**
     * 引擎活动标识。
     */
    private String engineActivityId;

    /**
     * 引擎执行标识。
     */
    private String executionId;

    /**
     * 引擎任务标识。
     */
    private String taskId;

    /**
     * 节点标识。
     */
    private String nodeId;

    /**
     * 源节点标识。
     */
    private String sourceNodeId;

    /**
     * 目标节点标识。
     */
    private String targetNodeId;

    /**
     * 画布连线标识。
     */
    private String edgeId;

    /**
     * 流转令牌标识。
     */
    private String tokenId;

    /**
     * 执行尝试次数。
     */
    private Integer attemptNo;

    /**
     * 节点类型。
     */
    private String nodeType;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 节点输入数据。
     */
    private Map<String, Object> input = new LinkedHashMap<>();

    /**
     * 节点输出数据。
     */
    private Map<String, Object> output = new LinkedHashMap<>();

    /**
     * 命中的后续端口。
     */
    private Map<String, Object> selectedPorts = new LinkedHashMap<>();

    /**
     * 输入载荷引用。
     */
    private String inputPayloadRef;

    /**
     * 输出载荷引用。
     */
    private String outputPayloadRef;

    /**
     * 事件展示摘要。
     */
    private Map<String, Object> eventSummary = new LinkedHashMap<>();

    /**
     * 错误编码。
     */
    private String errorCode;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 执行耗时毫秒数。
     */
    private Long elapsedMs;

    /**
     * 事件创建时间。
     */
    private Instant createTime;
}
