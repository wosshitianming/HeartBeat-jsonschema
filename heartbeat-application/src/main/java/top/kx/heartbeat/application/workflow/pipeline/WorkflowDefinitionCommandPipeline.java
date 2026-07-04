package top.kx.heartbeat.application.workflow.pipeline;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 工作流定义命令处理管道。
 *
 * <p>负责按顺序执行工作流定义创建前的命令处理器。</p>
 */
@Component
public class WorkflowDefinitionCommandPipeline {

    /**
     * 工作流定义命令处理器列表。
     */
    @Resource
    private List<WorkflowDefinitionCommandHandler> handlers;

    /**
     * 初始化命令处理器顺序。
     */
    @PostConstruct
    public void initialize() {
        // 按处理器 Order 值从小到大排序。
        Collections.sort(handlers, Comparator.comparingInt(WorkflowDefinitionCommandHandler::getOrder));
    }

    /**
     * 执行工作流定义命令处理链。
     *
     * @param command 工作流定义命令参数
     */
    public void handle(Map<String, Object> command) {
        // 创建本次链路上下文对象。
        WorkflowDefinitionCommandContext context = new WorkflowDefinitionCommandContext(command);
        // 逐个执行链路处理器。
        for (WorkflowDefinitionCommandHandler handler : handlers) {
            // 将共享上下文交给当前处理器处理。
            handler.handle(context);
        }
    }
}
