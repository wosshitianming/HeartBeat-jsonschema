package top.kx.heartbeat.infrastructure.flow.flowable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.RepositoryService;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.flow.runtime.FlowResumeCommand;
import top.kx.heartbeat.application.flow.runtime.FlowStartCommand;
import top.kx.heartbeat.application.flow.runtime.NodeExecutionOutcome;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flowable 变量编解码器。
 *
 * <p>用于集中控制 Flowable 变量名称、payload 瘦身和节点执行结果写回策略。</p>
 */
@Component
public class FlowableVariableCodec {

    /**
     * 运行标识变量名。
     */
    public static final String RUN_ID = "hbRunId";

    /**
     * 流程定义标识变量名。
     */
    public static final String FLOW_ID = "hbFlowId";

    /**
     * 租户标识变量名。
     */
    public static final String TENANT_ID = "hbTenantId";

    /**
     * Payload 变量名。
     */
    public static final String PAYLOAD = "payload";

    /**
     * 命中端口变量名。
     */
    public static final String NEXT_PORTS = "hbNextPorts";

    /**
     * Payload 瘦身存储。
     */
    @Resource
    private FlowablePayloadStore payloadStore;

    /**
     * Flowable 仓储服务。
     */
    @Resource
    private RepositoryService repositoryService;

    /**
     * 编码流程启动变量。
     *
     * @param command 流程启动命令
     * @return Flowable 启动变量
     */
    public Map<String, Object> toStartVariables(FlowStartCommand command) {
        // 创建变量映射。
        Map<String, Object> variables = new LinkedHashMap<>();
        // 写入流程定义标识。
        variables.put(FLOW_ID, command.getFlowId());
        // 写入运行标识。
        variables.put(RUN_ID, command.getRunId());
        // 写入租户标识。
        variables.put(TENANT_ID, command.getTenantId());
        // 写入触发类型。
        variables.put("hbTriggerType", command.getTriggerType().getCode());
        // 写入幂等键。
        variables.put("hbIdempotencyKey", command.getIdempotencyKey());
        // 写入业务键。
        variables.put("hbBusinessKey", command.getBusinessKey());
        // 写入关联键。
        variables.put("hbCorrelationKey", command.getCorrelationKey());
        // 写入瘦身后的 payload。
        variables.put(PAYLOAD, payloadStore.slim(command.getPayload()));
        // 返回变量映射。
        return variables;
    }

    /**
     * 编码流程恢复变量。
     *
     * @param command 流程恢复命令
     * @return Flowable 恢复变量
     */
    public Map<String, Object> toResumeVariables(FlowResumeCommand command) {
        // 创建变量映射。
        Map<String, Object> variables = new LinkedHashMap<>();
        // 写入租户标识。
        variables.put(TENANT_ID, command.getTenantId());
        // 写入运行标识。
        variables.put(RUN_ID, command.getRunId());
        // 写入等待实例标识。
        variables.put("hbWaitInstanceId", command.getWaitInstanceId());
        // 写入关联键。
        variables.put("hbCorrelationKey", command.getCorrelationKey());
        // 写入恢复 payload。
        variables.put(PAYLOAD, payloadStore.slim(command.getPayload()));
        // 返回变量映射。
        return variables;
    }

    /**
     * 从执行上下文读取 payload。
     *
     * @param execution Flowable 执行上下文
     * @return payload 数据
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readPayload(DelegateExecution execution) {
        // 读取 payload 变量。
        Object payload = execution.getVariable(PAYLOAD);
        // 判断 payload 是否为 Map。
        if (payload instanceof Map) {
            // 返回 payload 映射。
            return new LinkedHashMap<>((Map<String, Object>) payload);
        }
        // 返回空 payload。
        return new LinkedHashMap<>();
    }

    /**
     * 从执行上下文读取流程变量。
     *
     * @param execution Flowable 执行上下文
     * @return 流程变量
     */
    public Map<String, Object> readVariables(DelegateExecution execution) {
        // 创建变量映射。
        Map<String, Object> variables = new LinkedHashMap<>();
        // 遍历 Flowable 变量名。
        for (String name : execution.getVariableNames()) {
            // 写入变量值。
            variables.put(name, execution.getVariable(name));
        }
        // 返回变量映射。
        return variables;
    }

    /**
     * 解析当前节点标识。
     *
     * @param execution Flowable 执行上下文
     * @return 当前节点标识
     */
    public String resolveNodeId(DelegateExecution execution) {
        // 读取扩展字段中的节点标识。
        String nodeId = readFieldExtension(execution, "flowNodeId");
        // 判断扩展字段是否存在。
        if (StringUtils.isNotBlank(nodeId)) {
            // 返回扩展字段节点标识。
            return nodeId;
        }
        // 返回活动标识兜底。
        return StringUtils.removeStart(execution.getCurrentActivityId(), "node_");
    }

    /**
     * 解析当前节点类型。
     *
     * @param execution Flowable 执行上下文
     * @return 当前节点类型
     */
    public String resolveNodeType(DelegateExecution execution) {
        // 读取扩展字段中的节点类型。
        String nodeType = readFieldExtension(execution, "flowNodeType");
        // 返回节点类型或节点标识兜底。
        return StringUtils.defaultIfBlank(nodeType, resolveNodeId(execution));
    }

    /**
     * 解析当前节点执行器标识。
     *
     * @param execution Flowable 执行上下文
     * @param fallback 节点类型兜底
     * @return 节点执行器标识
     */
    public String resolveExecutorId(DelegateExecution execution, String fallback) {
        // 读取扩展字段中的执行器标识。
        String executorId = readFieldExtension(execution, "flowExecutorId");
        // 返回执行器标识或节点类型兜底。
        return StringUtils.defaultIfBlank(executorId, fallback);
    }

    /**
     * 读取服务任务扩展字段。
     *
     * @param execution Flowable 执行上下文
     * @param fieldName 字段名称
     * @return 字段值
     */
    private String readFieldExtension(DelegateExecution execution, String fieldName) {
        // 读取 BPMN 模型。
        BpmnModel model = repositoryService.getBpmnModel(execution.getProcessDefinitionId());
        // 判断 BPMN 模型是否存在。
        if (model == null) {
            // 返回空字段值。
            return "";
        }
        // 读取当前活动元素。
        FlowElement element = model.getFlowElement(execution.getCurrentActivityId());
        // 判断当前活动是否为服务任务。
        if (!(element instanceof ServiceTask)) {
            // 返回空字段值。
            return "";
        }
        // 转换为服务任务。
        ServiceTask serviceTask = (ServiceTask) element;
        // 遍历服务任务扩展字段。
        for (FieldExtension field : serviceTask.getFieldExtensions()) {
            // 判断字段名称是否匹配。
            if (fieldName.equals(field.getFieldName())) {
                // 返回字段字符串值。
                return StringUtils.defaultString(field.getStringValue());
            }
        }
        // 未找到字段时返回空字符串。
        return "";
    }

    /**
     * 写回节点执行输出。
     *
     * @param execution Flowable 执行上下文
     * @param outcome 节点执行输出
     */
    public void writeOutcome(DelegateExecution execution, NodeExecutionOutcome outcome) {
        // 写入瘦身后的 payload。
        execution.setVariable(PAYLOAD, payloadStore.slim(outcome.getOutput()));
        // 写入命中端口。
        execution.setVariable(NEXT_PORTS, new ArrayList<>(outcome.getNextPorts()));
        // 写入节点状态。
        execution.setVariable("hbLastNodeStatus", outcome.getStatus());
        // 写入节点错误编码。
        execution.setVariable("hbLastErrorCode", outcome.getErrorCode());
        // 写入节点错误信息。
        execution.setVariable("hbLastErrorMessage", outcome.getErrorMessage());
    }

    /**
     * 读取命中端口。
     *
     * @param execution Flowable 执行上下文
     * @return 命中端口列表
     */
    @SuppressWarnings("unchecked")
    public List<String> readNextPorts(DelegateExecution execution) {
        // 读取命中端口变量。
        Object nextPorts = execution.getVariable(NEXT_PORTS);
        // 判断命中端口是否为列表。
        if (nextPorts instanceof List) {
            // 返回命中端口列表。
            return (List<String>) nextPorts;
        }
        // 返回空列表。
        return new ArrayList<>();
    }
}
