package top.kx.heartbeat.infrastructure.flow.flowable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.flow.runtime.NodeExecutionOutcome;
import top.kx.heartbeat.domain.flow.model.FlowNode;
import top.kx.heartbeat.domain.flow.model.FlowNodeRunStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 外部 I/O 命令派发器。
 *
 * <p>用于把 HTTP、MySQL、Redis、MQ 等长耗时节点转换为外部工作者命令。</p>
 */
@Service
public class FlowExternalIoCommandDispatcher {

    /**
     * 判断节点是否需要外部 I/O 模式。
     *
     * @param node 流程节点
     * @return 是否需要外部 I/O 模式
     */
    public boolean supports(FlowNode node) {
        // 读取节点类型。
        String type = node.getType();
        // 判断节点类型是否属于外部 I/O。
        return StringUtils.contains(type, ".http.") || StringUtils.contains(type, ".mysql.") || StringUtils.contains(type, ".redis.") || StringUtils.contains(type, ".mq.")
                || StringUtils.contains(type, ":http.") || StringUtils.contains(type, ":mysql.") || StringUtils.contains(type, ":redis.") || StringUtils.contains(type, ":mq.");
    }

    /**
     * 派发外部 I/O 命令。
     *
     * @param execution Flowable 执行上下文
     * @param node 流程节点
     * @param payload 输入载荷
     * @return 节点执行输出
     */
    public NodeExecutionOutcome dispatch(DelegateExecution execution, FlowNode node, Map<String, Object> payload) {
        // 创建命令摘要。
        Map<String, Object> output = new LinkedHashMap<>();
        // 写入命令状态。
        output.put("commandStatus", "CREATED");
        // 写入流程实例标识。
        output.put("processInstanceId", execution.getProcessInstanceId());
        // 写入节点标识。
        output.put("nodeId", node.getId());
        // 创建节点输出。
        NodeExecutionOutcome outcome = new NodeExecutionOutcome();
        // 标记节点等待外部工作者。
        outcome.setStatus(FlowNodeRunStatus.WAITING.getCode());
        // 写入输出摘要。
        outcome.setOutput(output);
        // 写入等待端口。
        outcome.setNextPorts(Collections.singletonList("wait"));
        // 返回节点输出。
        return outcome;
    }
}
