package top.kx.heartbeat.application.flow.runtime;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.domain.flow.model.*;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;
import top.kx.heartbeat.domain.flow.repository.NodeComponentRepository;
import top.kx.heartbeat.domain.flow.validation.FlowDslValidator;
import top.kx.heartbeat.domain.flow.validation.FlowValidationResult;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 流程调试执行器。
 *
 * <p>负责按流程 DSL 推进节点执行、记录节点事件并保存调试运行记录。</p>
 */
@Service
public class FlowExecutor {

    /**
     * 节点组件仓储。
     */
    @Resource
    private NodeComponentRepository componentRepository;

    /**
     * 流程运行记录仓储。
     */
    @Resource
    private FlowRunRepository flowRunRepository;

    /**
     * 节点执行器注册表。
     */
    @Resource
    private NodeExecutorRegistry nodeExecutorRegistry;

    /**
     * 流程 DSL 校验领域服务。
     */
    @Resource
    private FlowDslValidator validator;

    /**
     * 初始化内置节点执行器索引。
     *
     * 调试执行流程定义。
     *
     * @param flow 流程定义
     * @param input 调试输入数据
     * @return 流程调试结果
     */
    public FlowDebugResult debug(FlowDefinition flow, Map<String, Object> input) {
        // 查询全部启用节点组件。
        List<NodeComponentManifest> manifests = componentRepository.findAllActive();
        // 校验流程定义。
        FlowValidationResult validation = validator.validate(flow, manifests);
        // 判断流程定义是否有效。
        if (!validation.isValid()) {
            // 抛出第一条校验失败原因。
            throw new IllegalArgumentException("流程编译失败: " + validation.getIssues().get(0).getMessage());
        }

        // 生成流程运行标识。
        String runId = nextNumericId();
        // 记录流程开始时间。
        Instant startedAt = Instant.now();
        // 构建节点标识索引。
        Map<String, FlowNode> nodes = flow.getNodes().stream().collect(Collectors.toMap(FlowNode::getId, item -> item));
        // 构建节点组件清单索引。
        Map<String, NodeComponentManifest> manifestByNodeId = manifestsByNodeId(flow, manifests);
        // 构建源节点连线索引。
        Map<String, List<FlowEdge>> edgesBySource = edgesBySource(flow);
        // 创建流程运行事件列表。
        List<FlowRunEvent> events = new ArrayList<>();
        // 创建待执行节点队列。
        Queue<NodePayload> queue = new ArrayDeque<>();

        // 遍历流程节点寻找入口节点。
        for (FlowNode node : flow.getNodes()) {
            // 读取节点组件清单。
            NodeComponentManifest manifest = manifestByNodeId.get(node.getId());
            // 无输入端口的节点作为入口节点。
            if (manifest != null && CollectionUtils.isEmpty(manifest.getPorts().getInputs())) {
                // 添加入口节点到执行队列。
                queue.add(new NodePayload(node.getId(), input == null ? new LinkedHashMap<>() : input));
            }
        }
        // 兼容未配置入口节点的流程。
        if (CollectionUtils.isEmpty(queue) && CollectionUtils.isNotEmpty(flow.getNodes())) {
            // 将第一个节点作为默认入口。
            queue.add(new NodePayload(flow.getNodes().get(0).getId(), input == null ? new LinkedHashMap<>() : input));
        }

        // 初始化最后输出数据。
        Map<String, Object> lastOutput = new LinkedHashMap<>();
        // 循环执行待处理节点。
        while (CollectionUtils.isNotEmpty(queue)) {
            // 弹出当前节点负载。
            NodePayload payload = queue.poll();
            // 查询当前流程节点。
            FlowNode node = nodes.get(payload.nodeId);
            // 查询当前节点组件清单。
            NodeComponentManifest manifest = manifestByNodeId.get(node.getId());
            // 查询当前节点执行器。
            NodeExecutor executor = nodeExecutorRegistry.getRequired(manifest.getRuntime().getExecutor());
            // 记录节点开始时间戳。
            long started = System.currentTimeMillis();
            // 执行当前节点。
            NodeExecutionResult result = executor.execute(new NodeExecutionContext(runId, node, payload.input, flow.getVariables()));
            // 计算节点执行耗时。
            long elapsed = System.currentTimeMillis() - started;
            // 更新最后输出数据。
            lastOutput = result.getOutput();
            // 添加节点运行事件。
            // 构建节点运行事件。
            FlowRunEvent event = new FlowRunEvent();
            // 写入运行事件标识。
            event.setId(nextNumericId());
            // 写入流程运行标识。
            event.setRunId(runId);
            // 写入节点标识。
            event.setNodeId(node.getId());
            // 写入源节点标识。
            event.setSourceNodeId(node.getId());
            // 写入节点类型。
            event.setNodeType(node.getType());
            // 写入事件类型。
            event.setEventType(result.getStatus());
            // 写入节点输入。
            event.setInput(payload.input);
            // 写入节点输出。
            event.setOutput(result.getOutput());
            // 写入错误信息。
            event.setErrorMessage(result.getErrorMessage());
            // 写入执行耗时。
            event.setElapsedMs(elapsed);
            // 写入事件创建时间。
            event.setCreateTime(Instant.now());
            // 添加节点运行事件。
            events.add(event);
            // 保存最新节点运行事件。
            flowRunRepository.saveEvent(events.get(events.size() - 1));
            // 遍历当前节点的出边。
            for (FlowEdge edge : edgesBySource.getOrDefault(node.getId(), new ArrayList<>())) {
                // 命中后续端口时推进目标节点。
                if (result.getNextPorts().contains(edge.getSourcePort())) {
                    // 将目标节点加入执行队列。
                    queue.add(new NodePayload(edge.getTarget(), result.getOutput()));
                }
            }
        }

        // 记录流程结束时间。
        Instant finishedAt = Instant.now();
        // 保存流程运行记录。
        // 构建流程运行记录。
        FlowRun run = new FlowRun();
        // 写入流程运行标识。
        run.setId(runId);
        // 写入流程定义标识。
        run.setFlowId(flow.getId());
        // 写入流程版本号。
        run.setVersionNo(flow.getActiveVersionNo() == null ? 0 : flow.getActiveVersionNo());
        // 写入运行时引擎。
        run.setEngine(FlowRuntimeEngine.LOCAL_DEBUG.getCode());
        // 写入触发类型。
        run.setTriggerType(FlowRunTriggerType.DEBUG.getCode());
        // 写入幂等范围。
        run.setIdempotencyScope(FlowIdempotencyScope.START.getCode());
        // 写入运行状态。
        run.setStatus(FlowRunStatus.SUCCESS.getCode());
        // 写入输入摘要。
        run.setInputSummary(input == null ? new LinkedHashMap<>() : input);
        // 写入输出摘要。
        run.setOutputSummary(lastOutput);
        // 写入开始时间。
        run.setStartedAt(startedAt);
        // 写入结束时间。
        run.setFinishedAt(finishedAt);
        // 写入执行耗时。
        run.setElapsedMs(finishedAt.toEpochMilli() - startedAt.toEpochMilli());
        // 保存流程运行记录。
        flowRunRepository.saveRun(run);
        // 返回流程调试结果。
        return new FlowDebugResult(runId, FlowRunStatus.SUCCESS.getCode(), lastOutput, events);
    }

    /**
     * 按节点标识索引组件清单。
     *
     * @param flow 流程定义
     * @param manifests 组件清单列表
     * @return 节点标识与组件清单映射
     */
    private Map<String, NodeComponentManifest> manifestsByNodeId(FlowDefinition flow, List<NodeComponentManifest> manifests) {
        // 按组件类型与版本构建组件清单索引。
        Map<String, NodeComponentManifest> byKey = manifests.stream()
                .collect(Collectors.toMap(item -> FlowDslValidator.manifestKey(item.getType(), item.getVersion()), item -> item));
        // 创建节点标识与组件清单映射。
        Map<String, NodeComponentManifest> result = new HashMap<>();
        // 遍历流程节点。
        for (FlowNode node : flow.getNodes()) {
            // 写入节点对应的组件清单。
            result.put(node.getId(), byKey.get(FlowDslValidator.manifestKey(node.getType(), node.getVersion())));
        }
        // 返回节点组件清单索引。
        return result;
    }

    /**
     * 按源节点索引流程连线。
     *
     * @param flow 流程定义
     * @return 源节点与出边列表映射
     */
    private Map<String, List<FlowEdge>> edgesBySource(FlowDefinition flow) {
        // 创建源节点连线索引。
        Map<String, List<FlowEdge>> result = new HashMap<>();
        // 判断流程连线是否为空。
        if (flow.getEdges() == null) {
            // 返回空连线索引。
            return result;
        }
        // 遍历流程连线。
        for (FlowEdge edge : flow.getEdges()) {
            // 按源节点收集连线。
            result.computeIfAbsent(edge.getSource(), key -> new ArrayList<>()).add(edge);
        }
        // 返回源节点连线索引。
        return result;
    }

    /**
     * 生成数据库友好的数字型字符串标识。
     *
     * @return 数字型字符串标识
     */
    private String nextNumericId() {
        // 生成毫秒级时间前缀。
        long prefix = System.currentTimeMillis() * 100000L;
        // 生成随机后缀。
        int suffix = ThreadLocalRandom.current().nextInt(100000);
        // 返回数字型字符串标识。
        return String.valueOf(prefix + suffix);
    }

    /**
     * 待执行节点负载。
     */
    private static class NodePayload {

        /**
         * 节点标识。
         */
        private final String nodeId;

        /**
         * 节点输入数据。
         */
        private final Map<String, Object> input;

        /**
         * 创建待执行节点负载。
         *
         * @param nodeId 节点标识
         * @param input 节点输入数据
         */
        private NodePayload(String nodeId, Map<String, Object> input) {
            // 绑定节点标识。
            this.nodeId = nodeId;
            // 绑定节点输入数据。
            this.input = input;
        }
    }
}
