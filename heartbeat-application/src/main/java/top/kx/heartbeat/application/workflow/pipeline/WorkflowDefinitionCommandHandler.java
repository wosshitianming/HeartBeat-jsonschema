package top.kx.heartbeat.application.workflow.pipeline;

import org.springframework.core.Ordered;

/**
 * 工作流定义命令处理器。
 *
 * <p>用于扩展工作流定义创建前的责任链节点。</p>
 */
public interface WorkflowDefinitionCommandHandler extends Ordered {

    /**
     * 处理工作流定义命令上下文。
     *
     * @param context 工作流定义命令上下文
     */
    void handle(WorkflowDefinitionCommandContext context);

    /**
     * 返回默认处理器顺序。
     *
     * @return 最低优先级
     */
    @Override
    default int getOrder() {
        // 默认放到责任链末尾执行。
        return Ordered.LOWEST_PRECEDENCE;
    }
}
