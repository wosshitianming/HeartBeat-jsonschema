package top.kx.heartbeat.infrastructure.flow.flowable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.flow.runtime.FlowResumeCommand;
import top.kx.heartbeat.application.flow.runtime.FlowStartCommand;
import top.kx.heartbeat.domain.flow.model.FlowIdempotencyScope;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunStatus;
import top.kx.heartbeat.domain.flow.model.FlowRuntimeEngine;

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

    @Resource
    private org.flowable.engine.RepositoryService repositoryService;

    /**
     * Flowable 变量编解码器。
     */
    @Resource
    private FlowableVariableCodec variableCodec;

    @Resource
    private FlowExternalIoCommandCancellationService externalIoCommandCancellationService;

    /**
     * 启动流程实例。
     *
     * @param command 流程启动命令
     * @return 流程运行记录
     */
    @Transactional
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
        run.setInputSummary(variableCodec.payloadForProjection(variables.get(FlowableVariableCodec.PAYLOAD)));
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
    @Transactional
    public void resume(FlowResumeCommand command) {
        // 判断执行标识是否存在。
        if (StringUtils.isBlank(command.getExecutionId())) {
            // 抛出等待执行标识缺失异常。
            throw new IllegalArgumentException("恢复 Flowable 等待实例需要 executionId");
        }
        Execution execution = runtimeService.createExecutionQuery()
                .executionId(command.getExecutionId())
                .singleResult();
        if (execution == null) {
            throw new IllegalArgumentException("Flowable 等待 execution 不存在: " + command.getExecutionId());
        }
        String trustedTenantId = requiredVariable(command.getExecutionId(), FlowableVariableCodec.TENANT_ID);
        String trustedRunId = requiredVariable(command.getExecutionId(), FlowableVariableCodec.RUN_ID);
        validateResumeIdentity(command, execution, trustedTenantId, trustedRunId);
        Map<String, Object> variables = variableCodec.toResumeVariables(command, trustedTenantId, trustedRunId);
        // 发送消息恢复流程。
        runtimeService.messageEventReceived(command.getMessageName(), command.getExecutionId(), variables);
    }

    private String requiredVariable(String executionId, String name) {
        Object value = runtimeService.getVariable(executionId, name);
        String text = value == null ? null : String.valueOf(value);
        if (StringUtils.isBlank(text)) {
            throw new IllegalStateException("Flowable execution 缺少可信变量 " + name);
        }
        return text;
    }

    private void validateResumeIdentity(FlowResumeCommand command, Execution execution,
                                        String trustedTenantId, String trustedRunId) {
        if (StringUtils.isBlank(command.getTenantId()) || !trustedTenantId.equals(command.getTenantId())) {
            throw new IllegalArgumentException("恢复命令 tenantId 与 Flowable execution 不一致");
        }
        if (StringUtils.isNotBlank(execution.getTenantId())
                && !trustedTenantId.equals(execution.getTenantId())) {
            throw new IllegalStateException("Flowable execution 原生租户与运行变量不一致");
        }
        if (StringUtils.isBlank(command.getRunId()) || !trustedRunId.equals(command.getRunId())) {
            throw new IllegalArgumentException("恢复命令 runId 与 Flowable execution 不一致");
        }
        if (StringUtils.isNotBlank(command.getEngineInstanceId())
                && !command.getEngineInstanceId().equals(execution.getProcessInstanceId())) {
            throw new IllegalArgumentException("恢复命令 processInstanceId 与 Flowable execution 不一致");
        }
        if (StringUtils.isBlank(command.getMessageName())) {
            throw new IllegalArgumentException("恢复 Flowable 等待实例需要 messageName");
        }
        if (StringUtils.isNotBlank(command.getWaitInstanceId())) {
            Object waitInstance = runtimeService.getVariableLocal(command.getExecutionId(), "hbWaitInstanceId");
            if (waitInstance == null || !command.getWaitInstanceId().equals(String.valueOf(waitInstance))) {
                throw new IllegalArgumentException("恢复命令 waitInstanceId 与 Flowable execution 不一致");
            }
        }
        Object ioCommandId = runtimeService.getVariable(command.getExecutionId(), FlowableVariableCodec.IO_COMMAND_ID);
        if (ioCommandId != null) {
            if (StringUtils.isBlank(execution.getTenantId())) {
                throw new IllegalStateException("外部 I/O execution 缺少 Flowable 原生租户");
            }
            if (StringUtils.isBlank(command.getEngineInstanceId())) {
                throw new IllegalArgumentException("恢复外部 I/O execution 必须携带可信 processInstanceId");
            }
            if (StringUtils.isBlank(command.getWaitInstanceId())) {
                throw new IllegalArgumentException("恢复外部 I/O execution 必须携带可信 waitInstanceId");
            }
            String trustedCorrelation = String.valueOf(runtimeService.getVariable(
                    command.getExecutionId(), FlowableVariableCodec.IO_CORRELATION_KEY));
            if (StringUtils.isBlank(command.getCorrelationKey())
                    || !command.getCorrelationKey().equals(trustedCorrelation)) {
                throw new IllegalArgumentException("恢复命令 correlationKey 与外部 I/O execution 不一致");
            }
            String trustedMessage = String.valueOf(runtimeService.getVariable(
                    command.getExecutionId(), FlowableVariableCodec.IO_MESSAGE_NAME));
            if (!command.getMessageName().equals(trustedMessage)) {
                throw new IllegalArgumentException("恢复命令 messageName 与外部 I/O execution 不一致");
            }
        }
    }

    /**
     * 取消流程实例。
     *
     * @param runId 流程运行标识
     * @param engineInstanceId 引擎流程实例标识
     * @param reason 取消原因
     */
    @Transactional
    public void cancel(String runId, String engineInstanceId, String reason) {
        // 判断引擎实例标识是否存在。
        if (StringUtils.isBlank(engineInstanceId)) {
            // 抛出引擎实例标识缺失异常。
            throw new IllegalArgumentException("取消 Flowable 流程需要 engineInstanceId");
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(engineInstanceId)
                .singleResult();
        if (instance == null) {
            throw new IllegalArgumentException("取消的 Flowable 流程实例不存在: " + engineInstanceId);
        }
        String trustedTenantId = requiredVariable(instance.getId(), FlowableVariableCodec.TENANT_ID);
        String trustedRunId = requiredVariable(instance.getId(), FlowableVariableCodec.RUN_ID);
        if (StringUtils.isBlank(instance.getTenantId()) || !instance.getTenantId().equals(trustedTenantId)) {
            throw new IllegalStateException("Flowable 流程实例原生租户与运行变量不一致");
        }
        if (StringUtils.isBlank(runId) || !runId.equals(trustedRunId)) {
            throw new IllegalArgumentException("取消命令 runId 与 Flowable 流程实例不一致");
        }
        externalIoCommandCancellationService.cancelByRun(
                trustedTenantId, trustedRunId, engineInstanceId, reason);
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
        if (StringUtils.isBlank(command.getTenantId())) {
            throw new IllegalArgumentException("启动 Flowable 流程需要 tenantId");
        }
        ProcessInstanceBuilder builder = runtimeService.createProcessInstanceBuilder()
                .tenantId(command.getTenantId())
                .businessKey(command.getBusinessKey())
                .variables(variables);
        if (StringUtils.isNotBlank(command.getProcessDefinitionId())) {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(command.getProcessDefinitionId())
                    .singleResult();
            if (definition == null) {
                throw new IllegalArgumentException("Flowable 流程定义不存在: " + command.getProcessDefinitionId());
            }
            if (!command.getTenantId().equals(definition.getTenantId())) {
                throw new IllegalArgumentException("Flowable 流程定义不属于当前租户");
            }
            return builder.processDefinitionId(command.getProcessDefinitionId()).start();
        } else if (StringUtils.isNotBlank(command.getProcessDefinitionKey())) {
            return builder.processDefinitionKey(command.getProcessDefinitionKey()).start();
        } else {
            // 抛出流程定义键缺失异常。
            throw new IllegalArgumentException("启动 Flowable 流程需要 processDefinitionId 或 processDefinitionKey");
        }
    }
}
