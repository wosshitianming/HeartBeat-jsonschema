package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.domain.flow.model.FlowTriggerType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程触发命令。
 *
 * <p>用于统一手动、Webhook、Cron、MQ 和领域事件入口的触发参数。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowTriggerCommand {

    /**
     * 租户标识。
     */
    private String tenantId;

    /**
     * 流程定义标识。
     */
    private String flowId;

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
     * 触发载荷。
     */
    private Map<String, Object> payload = new LinkedHashMap<>();
}
