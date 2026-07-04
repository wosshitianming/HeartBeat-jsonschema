package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.flow.runtime.NodeExecutionContext;
import top.kx.heartbeat.application.flow.runtime.NodeExecutionOutcome;
import top.kx.heartbeat.application.flow.runtime.NodeExecutionResult;
import top.kx.heartbeat.application.flow.runtime.NodeExecutor;
import top.kx.heartbeat.application.flow.runtime.NodeExecutorRegistry;
import top.kx.heartbeat.domain.flow.model.FlowNode;

import javax.annotation.Resource;

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
        // 判断是否为外部 I/O 节点。
        if (externalIoCommandDispatcher.supports(node)) {
            // 派发外部 I/O 命令。
            NodeExecutionOutcome outcome = externalIoCommandDispatcher.dispatch(execution, node, variableCodec.readPayload(execution));
            // 写回外部 I/O 等待输出。
            variableCodec.writeOutcome(execution, outcome);
            // 结束当前 delegate。
            return;
        }
        // 查询节点执行器。
        NodeExecutor executor = nodeExecutorRegistry.getRequired(variableCodec.resolveExecutorId(execution, node.getType()));
        // 构建节点执行上下文。
        NodeExecutionContext context = new NodeExecutionContext(execution.getProcessInstanceId(), node, variableCodec.readPayload(execution), variableCodec.readVariables(execution));
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
        // 返回流程节点。
        return node;
    }
}
