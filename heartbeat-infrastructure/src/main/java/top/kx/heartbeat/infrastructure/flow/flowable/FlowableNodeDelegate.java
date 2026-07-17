package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.flow.runtime.*;
import top.kx.heartbeat.domain.flow.model.FlowNode;

import javax.annotation.Resource;
import java.util.Map;

//import org.flowable.engine.delegate.Expression;

/**
 * Flowable 服务任务节点委托。
 *
 * <p>用于在 Flowable 推进到 serviceTask 时回调 HeartBeat 节点执行器。</p>
 */
@Service("flowableNodeDelegate")
public class FlowableNodeDelegate implements JavaDelegate {

    /**
     * Flowable 字段注入的节点标识。
     */
    @SuppressWarnings("unused")
    private Expression flowNodeId;

    /**
     * Flowable 字段注入的节点类型。
     */
    @SuppressWarnings("unused")
    private Expression flowNodeType;

    /**
     * Flowable 字段注入的执行器标识。
     */
    @SuppressWarnings("unused")
    private Expression flowExecutorId;

    @SuppressWarnings("unused")
    private Expression flowNodeVersion;

    @SuppressWarnings("unused")
    private Expression flowNodeConfigBase64;

    /**
     * Flowable 字段注入的运行模式。
     */
    @SuppressWarnings("unused")
    private Expression runtimeMode;

    /**
     * 节点执行器注册表。
     */
    @Resource
    private NodeExecutorRegistry nodeExecutorRegistry;

    /**
     * Flowable 变量编解码器。
     */
    @Resource
    private FlowableVariableCodec variableCodec;

    /**
     * 外部 I/O 命令派发器。
     */
    @Resource
    private FlowExternalIoCommandDispatcher externalIoCommandDispatcher;

    /**
     * 执行 Flowable 服务任务。
     *
     * @param execution Flowable 执行上下文
     */
    @Override
    public void execute(DelegateExecution execution) {
        // 构建 HeartBeat 节点。
        FlowNode node = createNode(execution);
        Map<String, Object> payload = variableCodec.readPayload(execution);
        String runtimeMode = variableCodec.resolveRuntimeMode(execution);
        if ("EXTERNAL_IO_PREPARE".equals(runtimeMode)) {
            externalIoCommandDispatcher.dispatch(execution, node, payload);
            return;
        }
        if ("EXTERNAL_IO_RESULT".equals(runtimeMode)) {
            variableCodec.writeOutcome(execution, externalIoCommandDispatcher.applyResult(execution, node));
            return;
        }
        if ("EXTERNAL_IO".equals(runtimeMode) || externalIoCommandDispatcher.supports(node)) {
            throw new IllegalStateException("旧版 EXTERNAL_IO BPMN 不包含等待节点，请重新发布流程版本");
        }
        // 查询节点执行器。
        NodeExecutor executor = nodeExecutorRegistry.getRequired(variableCodec.resolveExecutorId(execution, node.getType()));
        // 构建节点执行上下文。
        NodeExecutionContext context = new NodeExecutionContext(
                variableCodec.readRunId(execution), node, payload, variableCodec.readVariables(execution, payload));
        // 执行 HeartBeat 节点。
        NodeExecutionResult result = executor.execute(context);
        // 写回节点执行输出。
        variableCodec.writeOutcome(execution, NodeExecutionOutcome.from(result));
    }

    /**
     * 创建 HeartBeat 节点。
     *
     * @param execution Flowable 执行上下文
     * @return HeartBeat 节点
     */
    private FlowNode createNode(DelegateExecution execution) {
        // 创建流程节点。
        FlowNode node = new FlowNode();
        // 写入节点标识。
        node.setId(variableCodec.resolveNodeId(execution));
        // 写入节点类型。
        node.setType(variableCodec.resolveNodeType(execution));
        node.setVersion(variableCodec.resolveNodeVersion(execution));
        node.setConfig(variableCodec.resolveNodeConfig(execution));
        // 返回流程节点。
        return node;
    }
}
