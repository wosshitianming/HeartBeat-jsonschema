package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程恢复命令。
 *
 * <p>用于把外部事件、人工任务或等待消息恢复为运行时继续推进动作。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowResumeCommand {

    /**
     * 租户标识。
     */
    private String tenantId;

    /**
     * 流程运行标识。
     */
    private String runId;

    /**
     * 引擎流程实例标识。
     */
    private String engineInstanceId;

    /**
     * 引擎执行标识。
     */
    private String executionId;

    /**
     * 等待实例标识。
     */
    private String waitInstanceId;

    /**
     * 消息名称。
     */
    private String messageName;

    /**
     * 关联键。
     */
    private String correlationKey;

    /**
     * 恢复载荷。
     */
    private Map<String, Object> payload = new LinkedHashMap<>();
}
