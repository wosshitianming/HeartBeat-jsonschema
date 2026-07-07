package top.kx.heartbeat.infrastructure.flow.flowable;

import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.flow.runtime.FlowResumeCommand;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Flowable 外部事件桥接服务。
 *
 * <p>用于把 Webhook、MQ 或领域事件转换为 Flowable 消息恢复动作。</p>
 */
@Service
public class FlowableEventBridge {

    /**
     * Flowable 运行服务。
     */
    @Resource
    private FlowableRuntimeService runtimeService;

    /**
     * 恢复消息等待流程。
     *
     * @param tenantId 租户标识
     * @param runId 运行标识
     * @param executionId 执行标识
     * @param messageName 消息名称
     * @param payload 恢复载荷
     */
    public void resumeMessage(String tenantId,
                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                              String runId,
                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                              String executionId,
                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                              String messageName,
                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                              Map<String, Object> payload) {
        // 创建恢复命令。
        FlowResumeCommand command = new FlowResumeCommand();
        // 写入租户标识。
        command.setTenantId(tenantId);
        // 写入运行标识。
        command.setRunId(runId);
        // 写入执行标识。
        command.setExecutionId(executionId);
        // 写入消息名称。
        command.setMessageName(messageName);
        // 写入恢复载荷。
        command.setPayload(payload);
        // 恢复 Flowable 流程。
        runtimeService.resume(command);
    }
}
