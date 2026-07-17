package top.kx.heartbeat.application.flow.runtime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.domain.flow.model.ComponentRuntime;
import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;
import top.kx.heartbeat.domain.flow.model.NodeComponentSource;
import top.kx.heartbeat.domain.flow.model.NodeComponentStatus;
import top.kx.heartbeat.domain.flow.validation.FlowDslValidator;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

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
     * Java 代码节点清单索引。
     */
    private final Map<String, NodeComponentManifest> codeManifestIndex = new LinkedHashMap<>();

    /**
     * 执行器标识索引。
     */
    private final Map<String, NodeExecutor> executors = new LinkedHashMap<>();
    /**
     * Spring 托管的扩展节点执行器。
     */
    @Resource
    private List<NodeExecutor> springExecutors;

    /**
     * 初始化节点执行器注册表。
     */
    @PostConstruct
    public void initialize() {
        // 清空旧的执行器索引。
        executors.clear();
        codeManifestIndex.clear();
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
     * 判断执行器是否已注册。
     *
     * @param executorId 执行器标识
     * @return 是否已注册
     */
    public boolean contains(String executorId) {
        return StringUtils.isNotBlank(executorId) && executors.containsKey(executorId.trim());
    }

    /**
     * 返回 Spring 自动发现的 Java 代码节点清单。
     *
     * @return 代码节点清单
     */
    public List<NodeComponentManifest> codeManifests() {
        return Collections.unmodifiableList(new ArrayList<>(codeManifestIndex.values()));
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
            register(item);
        }
    }

    private void register(NodeExecutor executor) {
        String executorId = StringUtils.trimToNull(executor.executorId());
        if (executorId == null) {
            throw new IllegalStateException("节点执行器标识不能为空: " + executor.getClass().getName());
        }
        NodeExecutor existing = executors.get(executorId);
        if (existing != null && existing != executor) {
            throw new IllegalStateException("节点执行器标识重复: " + executorId
                    + " (" + existing.getClass().getName() + ", " + executor.getClass().getName() + ")");
        }
        executors.put(executorId, executor);
        if (executor instanceof CodeNode) {
            registerCodeManifest((CodeNode) executor, executorId);
        }
    }

    private void registerCodeManifest(CodeNode codeNode, String executorId) {
        NodeComponentManifest manifest = codeNode.manifest();
        if (manifest == null) {
            throw new IllegalStateException("代码节点 Manifest 不能为空: " + executorId);
        }
        if (StringUtils.isBlank(manifest.getType())) {
            throw new IllegalStateException("代码节点类型不能为空: " + executorId);
        }
        if (StringUtils.isBlank(manifest.getVersion())) {
            manifest.setVersion("1.0.0");
        }
        if (StringUtils.isBlank(manifest.getSource())) {
            manifest.setSource(NodeComponentSource.CODE.getCode());
        }
        if (StringUtils.isBlank(manifest.getStatus())) {
            manifest.setStatus(NodeComponentStatus.ACTIVE.getCode());
        }
        ComponentRuntime runtime = manifest.getRuntime();
        if (runtime == null) {
            runtime = new ComponentRuntime();
            manifest.setRuntime(runtime);
        }
        if (StringUtils.isBlank(runtime.getExecutor())) {
            runtime.setExecutor(executorId);
        }
        if (!executorId.equals(runtime.getExecutor())) {
            throw new IllegalStateException("代码节点 Manifest 执行器不匹配: " + manifest.getType()
                    + " 声明 " + runtime.getExecutor() + "，实际为 " + executorId);
        }
        String key = FlowDslValidator.manifestKey(manifest.getType(), manifest.getVersion());
        NodeComponentManifest existing = codeManifestIndex.putIfAbsent(key, manifest);
        if (existing != null && existing != manifest) {
            throw new IllegalStateException("代码节点类型与版本重复: " + key);
        }
    }
}
