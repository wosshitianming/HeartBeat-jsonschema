package top.kx.heartbeat.application.workflow.pipeline;

import java.util.Map;

/**
 * 工作流定义命令上下文。
 *
 * <p>用于在责任链处理器之间传递共享命令参数。</p>
 */
public class WorkflowDefinitionCommandContext {

    /**
     * 工作流定义命令参数。
     */
    private final Map<String, Object> command;

    /**
     * 创建工作流定义命令上下文。
     *
     * @param command 工作流定义命令参数
     */
    public WorkflowDefinitionCommandContext(Map<String, Object> command) {
        // 写入工作流定义命令参数。
        this.command = command;
    }

    public String stringValue(String key) {
        Object value = command.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public void put(String key, Object value) {
        command.put(key, value);
    }

    public void putIfBlank(String key, String value) {
        if ((stringValue(key).isEmpty()) && value != null && !value.trim().isEmpty()) {
            command.put(key, value.trim());
        }
    }
}
