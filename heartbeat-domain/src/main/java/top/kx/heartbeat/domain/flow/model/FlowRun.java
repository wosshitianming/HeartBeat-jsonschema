package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程运行领域模型。
 *
 * <p>用于记录流程一次运行的输入输出摘要、状态和耗时。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowRun {

    /**
     * 流程运行标识。
     */
    private String id;

    /**
     * 流程定义标识。
     */
    private String flowId;

    /**
     * 流程运行版本号。
     */
    private int versionNo;

    /**
     * 运行编号。
     */
    private String runNo;

    /**
     * 运行时引擎。
     */
    private String engine;

    /**
     * 引擎流程实例标识。
     */
    private String engineInstanceId;

    /**
     * 引擎流程定义标识。
     */
    private String processDefinitionId;

    /**
     * 流程版本标识。
     */
    private String flowVersionId;

    /**
     * 触发器标识。
     */
    private String triggerId;

    /**
     * 触发器键。
     */
    private String triggerKey;

    /**
     * 流程触发类型。
     */
    private String triggerType;

    /**
     * 幂等键。
     */
    private String idempotencyKey;

    /**
     * 幂等范围。
     */
    private String idempotencyScope;

    /**
     * 业务键。
     */
    private String businessKey;

    /**
     * 关联键。
     */
    private String correlationKey;

    /**
     * 直接父运行标识。
     */
    private String parentRunId;

    /**
     * 根运行标识。
     */
    private String rootRunId;

    /**
     * 复原重试来源运行标识。
     */
    private String retryFromRunId;

    /**
     * 重试序号。
     */
    private Integer retryNo;

    /**
     * 重试原因。
     */
    private String retryReason;

    /**
     * 租户标识。
     */
    private String tenantId;

    /**
     * 流程运行状态。
     */
    private String status;

    /**
     * 输入摘要。
     */
    private Map<String, Object> inputSummary = new LinkedHashMap<>();

    /**
     * 输出摘要。
     */
    private Map<String, Object> outputSummary = new LinkedHashMap<>();

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 开始时间。
     */
    private Instant startedAt;

    /**
     * 结束时间。
     */
    private Instant finishedAt;

    /**
     * 执行耗时毫秒数。
     */
    private Long elapsedMs;
}
