package top.kx.heartbeat.application.flow.runtime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行器注册表。
 *
 * <p>用于统一管理内置执行器和 Spring 托管的扩展执行器，避免各运行时重复维护执行器索引。</p>
 */
@Service
public class NodeExecutorRegistry {

    /**
     * 内置节点执行器工厂。
     */
    @Resource
    private BuiltinNodeExecutors builtins;

    /**
     * Spring 托管的扩展节点执行器。
     */
    @Autowired(required = false)
    private List<NodeExecutor> springExecutors;

    /**
     * 执行器标识索引。
     */
    private final Map<String, NodeExecutor> executors = new LinkedHashMap<>();

    /**
     * 初始化节点执行器注册表。
     */
    @PostConstruct
    public void initialize() {
        // 清空旧的执行器索引。
        executors.clear();
        // 注册内置节点执行器。
        registerAll(builtins.all());
        // 注册 Spring 托管的节点执行器。
        registerAll(springExecutors);
    }

    /**
     * 按执行器标识查询节点执行器。
     *
     * @param executorId 执行器标识
     * @return 节点执行器
     */
    public NodeExecutor getRequired(String executorId) {
        // 查询执行器实例。
        NodeExecutor executor = executors.get(executorId);
        // 判断执行器是否存在。
        if (executor == null) {
            // 抛出执行器缺失异常。
            throw new IllegalArgumentException("节点执行器不存在: " + executorId);
        }
        // 返回执行器实例。
        return executor;
    }

    /**
     * 注册节点执行器集合。
     *
     * @param items 节点执行器集合
     */
    private void registerAll(List<NodeExecutor> items) {
        // 判断执行器集合是否为空。
        if (items == null) {
            // 空集合无需注册。
            return;
        }
        // 遍历执行器集合。
        for (NodeExecutor item : items) {
            // 忽略空执行器。
            if (item == null) {
                // 继续处理下一个执行器。
                continue;
            }
            // 写入执行器标识索引。
            executors.put(item.executorId(), item);
        }
    }
}
