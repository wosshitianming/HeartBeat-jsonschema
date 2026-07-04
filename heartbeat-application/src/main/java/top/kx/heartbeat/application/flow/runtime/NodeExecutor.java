package top.kx.heartbeat.application.flow.runtime;

/**
 * 节点执行器接口。
 *
 * <p>用于承接 HeartBeat 节点组件在调试态和生产态 delegate 中的统一执行入口。</p>
 */
public interface NodeExecutor {

    /**
     * 获取节点执行器标识。
     *
     * @return 节点执行器标识
     */
    String executorId();

    /**
     * 执行当前节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    NodeExecutionResult execute(NodeExecutionContext context);
}
