package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.model.FlowTriggerType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程启动命令。
 *
 * <p>用于把应用层触发语义转换为生产运行时启动流程实例所需的参数。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowStartCommand {

    /**
     * 租户标识。
     */
    private String tenantId;

    /**
     * 流程定义标识。
     */
    private String flowId;

    /**
     * HeartBeat 流程运行标识。
     */
    private String runId;

    /**
     * 流程版本号。
     */
    private Integer versionNo;

    /**
     * 流程版本标识。
     */
    private String flowVersionId;

    /**
     * 运行时流程定义标识。
     */
    private String processDefinitionId;

    /**
     * 运行时流程定义键。
     */
    private String processDefinitionKey;

    /**
     * 触发器标识。
     */
    private String triggerId;

    /**
     * 触发器键。
     */
    private String triggerKey;

    /**
     * 触发类型。
     */
    private FlowTriggerType triggerType = FlowTriggerType.MANUAL;

    /**
     * 幂等键。
     */
    private String idempotencyKey;

    /**
     * 业务键。
     */
    private String businessKey;

    /**
     * 关联键。
     */
    private String correlationKey;

    /**
     * 本地调试兜底使用的流程定义快照。
     */
    private FlowDefinition flowDefinition;

    /**
     * 启动输入载荷。
     */
    private Map<String, Object> payload = new LinkedHashMap<>();
}
