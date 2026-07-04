package top.kx.heartbeat.infrastructure.flow.flowable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.flow.runtime.FlowResumeCommand;
import top.kx.heartbeat.application.flow.runtime.FlowStartCommand;
import top.kx.heartbeat.domain.flow.model.FlowIdempotencyScope;
import top.kx.heartbeat.domain.flow.model.FlowRuntimeEngine;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunStatus;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Map;

/**
 * Flowable 流程运行服务。
 *
 * <p>负责启动、恢复和取消 Flowable 流程实例。</p>
 */
@Service
public class FlowableRuntimeService {

    /**
     * Flowable 原生运行服务。
     */
    @Resource
    private org.flowable.engine.RuntimeService runtimeService;

    /**
     * Flowable 变量编解码器。
     */
    @Resource
    private FlowableVariableCodec variableCodec;

    /**
     * 启动流程实例。
     *
     * @param command 流程启动命令
     * @return 流程运行记录
     */
    public FlowRun start(FlowStartCommand command) {
        // 编码启动变量。
        Map<String, Object> variables = variableCodec.toStartVariables(command);
        // 启动流程实例。
        ProcessInstance instance = startProcessInstance(command, variables);
        // 创建流程运行记录。
        FlowRun run = new FlowRun();
        // 写入流程运行标识。
        run.setId(command.getRunId());
        // 写入流程定义标识。
        run.setFlowId(command.getFlowId());
        // 写入流程版本号。
        run.setVersionNo(command.getVersionNo() == null ? 0 : command.getVersionNo());
        // 写入流程版本标识。
        run.setFlowVersionId(command.getFlowVersionId());
        // 写入运行时引擎。
        run.setEngine(FlowRuntimeEngine.FLOWABLE.getCode());
        // 写入引擎实例标识。
        run.setEngineInstanceId(instance.getProcessInstanceId());
        // 写入流程定义标识。
        run.setProcessDefinitionId(instance.getProcessDefinitionId());
        // 写入触发器标识。
        run.setTriggerId(command.getTriggerId());
        // 写入触发器键。
        run.setTriggerKey(command.getTriggerKey());
        // 写入触发类型。
        run.setTriggerType(command.getTriggerType().getCode());
        // 写入幂等键。
        run.setIdempotencyKey(command.getIdempotencyKey());
        // 写入幂等范围。
        run.setIdempotencyScope(FlowIdempotencyScope.START.getCode());
        // 写入业务键。
        run.setBusinessKey(command.getBusinessKey());
        // 写入关联键。
        run.setCorrelationKey(command.getCorrelationKey());
        // 写入租户标识。
        run.setTenantId(command.getTenantId());
        // 写入运行状态。
        run.setStatus(FlowRunStatus.RUNNING.getCode());
        // 写入输入摘要。
        run.setInputSummary(command.getPayload());
        // 写入开始时间。
        run.setStartedAt(Instant.now());
        // 返回流程运行记录。
        return run;
    }

    /**
     * 恢复等待中的流程实例。
     *
     * @param command 流程恢复命令
     */
    public void resume(FlowResumeCommand command) {
        // 判断执行标识是否存在。
        if (StringUtils.isBlank(command.getExecutionId())) {
            // 抛出等待执行标识缺失异常。
            throw new IllegalArgumentException("恢复 Flowable 等待实例需要 executionId");
        }
        // 编码恢复变量。
        Map<String, Object> variables = variableCodec.toResumeVariables(command);
        // 发送消息恢复流程。
        runtimeService.messageEventReceived(command.getMessageName(), command.getExecutionId(), variables);
    }

    /**
     * 取消流程实例。
     *
     * @param runId 流程运行标识
     * @param engineInstanceId 引擎流程实例标识
     * @param reason 取消原因
     */
    public void cancel(String runId, String engineInstanceId, String reason) {
        // 判断引擎实例标识是否存在。
        if (StringUtils.isBlank(engineInstanceId)) {
            // 抛出引擎实例标识缺失异常。
            throw new IllegalArgumentException("取消 Flowable 流程需要 engineInstanceId");
        }
        // 删除 Flowable 流程实例。
        runtimeService.deleteProcessInstance(engineInstanceId, reason);
    }

    /**
     * 根据命令启动流程实例。
     *
     * @param command 流程启动命令
     * @param variables 启动变量
     * @return Flowable 流程实例
     */
    private ProcessInstance startProcessInstance(FlowStartCommand command, Map<String, Object> variables) {
        // 判断流程定义标识是否存在。
        if (StringUtils.isNotBlank(command.getProcessDefinitionId())) {
            // 按流程定义标识启动流程。
            return runtimeService.startProcessInstanceById(command.getProcessDefinitionId(), command.getBusinessKey(), variables);
        }
        // 判断流程定义键是否存在。
        if (StringUtils.isBlank(command.getProcessDefinitionKey())) {
            // 抛出流程定义键缺失异常。
            throw new IllegalArgumentException("启动 Flowable 流程需要 processDefinitionId 或 processDefinitionKey");
        }
        // 按流程定义键启动流程。
        return runtimeService.startProcessInstanceByKey(command.getProcessDefinitionKey(), command.getBusinessKey(), variables);
    }
}
