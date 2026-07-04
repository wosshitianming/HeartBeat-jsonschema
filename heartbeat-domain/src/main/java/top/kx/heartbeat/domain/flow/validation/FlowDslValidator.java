package top.kx.heartbeat.domain.flow.validation;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import top.kx.heartbeat.domain.flow.model.*;

import java.util.*;

/**
 * 流程 DSL 校验领域服务。
 *
 * <p>负责校验流程定义的节点、连线、组件清单和环路约束。</p>
 */
public class FlowDslValidator {

    /**
     * 校验流程定义。
     *
     * @param flow 流程定义
     * @param manifests 节点组件清单集合
     * @return 流程校验结果
     */
    public FlowValidationResult validate(FlowDefinition flow, Collection<NodeComponentManifest> manifests) {
        // 创建流程校验结果。
        FlowValidationResult result = new FlowValidationResult();
        // 判断流程定义是否为空。
        if (flow == null) {
            // 添加流程定义必填问题。
            result.add("FLOW_REQUIRED", "$", "流程定义不能为空");
            // 返回校验结果。
            return result;
        }
        // 校验流程节点并返回节点索引。
        Map<String, FlowNode> nodeById = validateNodes(flow, manifests, result);
        // 校验流程连线。
        validateEdges(flow, manifests, nodeById, result);
        // 校验流程环路。
        validateCycle(flow, result);
        // 返回流程校验结果。
        return result;
    }

    /**
     * 校验流程节点。
     *
     * @param flow 流程定义
     * @param manifests 节点组件清单集合
     * @param result 流程校验结果
     * @return 节点标识索引
     */
    private Map<String, FlowNode> validateNodes(FlowDefinition flow,
                                                Collection<NodeComponentManifest> manifests,
                                                FlowValidationResult result) {
        // 创建节点标识索引。
        Map<String, FlowNode> nodeById = new HashMap<>();
        // 创建组件清单索引。
        Map<String, NodeComponentManifest> manifestByKey = manifestByKey(manifests);
        // 判断流程节点是否为空。
        if (CollectionUtils.isEmpty(flow.getNodes())) {
            // 添加节点必填问题。
            result.add("NODE_REQUIRED", "$.nodes", "流程至少需要一个节点");
            // 返回空节点索引。
            return nodeById;
        }
        // 遍历流程节点。
        for (int i = 0; i < flow.getNodes().size(); i++) {
            // 读取当前流程节点。
            FlowNode node = flow.getNodes().get(i);
            // 构建当前节点 JSON 路径。
            String path = "$.nodes[" + i + "]";
            // 判断节点标识是否为空。
            if (isBlank(node.getId())) {
                // 添加节点标识必填问题。
                result.add("NODE_ID_REQUIRED", path + ".id", "节点 ID 不能为空");
                // 跳过当前无效节点。
                continue;
            }
            // 判断节点标识是否重复。
            if (nodeById.containsKey(node.getId())) {
                // 添加节点标识重复问题。
                result.add("DUPLICATE_NODE_ID", path + ".id", "节点 ID 重复: " + node.getId());
            }
            // 写入节点标识索引。
            nodeById.put(node.getId(), node);
            // 判断节点类型是否为空。
            if (isBlank(node.getType())) {
                // 添加节点类型必填问题。
                result.add("NODE_TYPE_REQUIRED", path + ".type", "节点类型不能为空");
                // 跳过当前无效节点。
                continue;
            }
            // 构建组件清单键。
            String key = manifestKey(node.getType(), node.getVersion());
            // 判断组件清单是否存在。
            if (!manifestByKey.containsKey(key)) {
                // 添加组件不存在问题。
                result.add("COMPONENT_NOT_FOUND", path + ".type", "组件未注册或未启用: " + key);
            }
        }
        // 返回节点标识索引。
        return nodeById;
    }

    /**
     * 校验流程连线。
     *
     * @param flow 流程定义
     * @param manifests 节点组件清单集合
     * @param nodeById 节点标识索引
     * @param result 流程校验结果
     */
    private void validateEdges(FlowDefinition flow,
                               Collection<NodeComponentManifest> manifests,
                               Map<String, FlowNode> nodeById,
                               FlowValidationResult result) {
        // 创建组件清单索引。
        Map<String, NodeComponentManifest> manifestByKey = manifestByKey(manifests);
        // 判断流程连线是否为空。
        if (flow.getEdges() == null) {
            // 空连线无需校验。
            return;
        }
        // 遍历流程连线。
        for (int i = 0; i < flow.getEdges().size(); i++) {
            // 读取当前流程连线。
            FlowEdge edge = flow.getEdges().get(i);
            // 构建当前连线 JSON 路径。
            String path = "$.edges[" + i + "]";
            // 查询源节点。
            FlowNode source = nodeById.get(edge.getSource());
            // 查询目标节点。
            FlowNode target = nodeById.get(edge.getTarget());
            // 判断源节点是否存在。
            if (source == null) {
                // 添加源节点不存在问题。
                result.add("EDGE_SOURCE_NOT_FOUND", path + ".source", "连线源节点不存在: " + edge.getSource());
                // 跳过当前无效连线。
                continue;
            }
            // 判断目标节点是否存在。
            if (target == null) {
                // 添加目标节点不存在问题。
                result.add("EDGE_TARGET_NOT_FOUND", path + ".target", "连线目标节点不存在: " + edge.getTarget());
                // 跳过当前无效连线。
                continue;
            }
            // 查询源节点组件清单。
            NodeComponentManifest sourceManifest = manifestByKey.get(manifestKey(source.getType(), source.getVersion()));
            // 查询目标节点组件清单。
            NodeComponentManifest targetManifest = manifestByKey.get(manifestKey(target.getType(), target.getVersion()));
            // 校验源端口是否存在。
            if (sourceManifest != null && !hasPort(sourceManifest.getPorts().getOutputs(), edge.getSourcePort())) {
                // 添加源端口不存在问题。
                result.add("EDGE_SOURCE_PORT_NOT_FOUND", path + ".sourcePort", "源端口不存在: " + edge.getSourcePort());
            }
            // 校验目标端口是否存在。
            if (targetManifest != null && !hasPort(targetManifest.getPorts().getInputs(), edge.getTargetPort())) {
                // 添加目标端口不存在问题。
                result.add("EDGE_TARGET_PORT_NOT_FOUND", path + ".targetPort", "目标端口不存在: " + edge.getTargetPort());
            }
        }
    }

    /**
     * 校验流程是否存在环路。
     *
     * @param flow 流程定义
     * @param result 流程校验结果
     */
    private void validateCycle(FlowDefinition flow, FlowValidationResult result) {
        // 创建流程邻接表。
        Map<String, Set<String>> adjacency = new HashMap<>();
        // 判断流程节点是否存在。
        if (flow.getNodes() != null) {
            // 遍历流程节点。
            for (FlowNode node : flow.getNodes()) {
                // 初始化节点邻接集合。
                adjacency.put(node.getId(), new HashSet<>());
            }
        }
        // 判断流程连线是否存在。
        if (flow.getEdges() != null) {
            // 遍历流程连线。
            for (FlowEdge edge : flow.getEdges()) {
                // 仅处理源节点和目标节点都存在的连线。
                if (adjacency.containsKey(edge.getSource()) && adjacency.containsKey(edge.getTarget())) {
                    // 写入邻接关系。
                    adjacency.get(edge.getSource()).add(edge.getTarget());
                }
            }
        }
        // 创建正在访问节点集合。
        Set<String> visiting = new HashSet<>();
        // 创建已访问节点集合。
        Set<String> visited = new HashSet<>();
        // 遍历邻接表节点。
        for (String nodeId : adjacency.keySet()) {
            // 判断当前节点是否存在环路。
            if (detectCycle(nodeId, adjacency, visiting, visited)) {
                // 添加流程环路问题。
                result.add("FLOW_HAS_CYCLE", "$.edges", "流程存在环路，MVP 暂不支持循环流程");
                // 命中环路后结束校验。
                return;
            }
        }
    }

    /**
     * 深度优先检测节点环路。
     *
     * @param nodeId 当前节点标识
     * @param adjacency 流程邻接表
     * @param visiting 正在访问节点集合
     * @param visited 已访问节点集合
     * @return 是否存在环路
     */
    private boolean detectCycle(String nodeId,
                                Map<String, Set<String>> adjacency,
                                Set<String> visiting,
                                Set<String> visited) {
        // 已访问节点不再重复检测。
        if (visited.contains(nodeId)) {
            // 已访问节点不存在新的环路。
            return false;
        }
        // 正在访问节点再次出现表示存在环路。
        if (visiting.contains(nodeId)) {
            // 返回存在环路。
            return true;
        }
        // 标记当前节点正在访问。
        visiting.add(nodeId);
        // 遍历当前节点后续节点。
        for (String next : adjacency.getOrDefault(nodeId, new HashSet<>())) {
            // 递归检测后续节点环路。
            if (detectCycle(next, adjacency, visiting, visited)) {
                // 后续节点存在环路。
                return true;
            }
        }
        // 移除正在访问标记。
        visiting.remove(nodeId);
        // 写入已访问节点集合。
        visited.add(nodeId);
        // 返回不存在环路。
        return false;
    }

    /**
     * 按组件清单键索引启用组件。
     *
     * @param manifests 节点组件清单集合
     * @return 组件清单键与组件清单映射
     */
    private Map<String, NodeComponentManifest> manifestByKey(Collection<NodeComponentManifest> manifests) {
        // 创建组件清单索引。
        Map<String, NodeComponentManifest> result = new HashMap<>();
        // 判断组件清单集合是否为空。
        if (manifests == null) {
            // 返回空组件清单索引。
            return result;
        }
        // 遍历组件清单集合。
        for (NodeComponentManifest manifest : manifests) {
            // 仅索引启用状态组件。
            if (NodeComponentStatus.ACTIVE.matches(manifest.getStatus())) {
                // 写入组件清单索引。
                result.put(manifestKey(manifest.getType(), manifest.getVersion()), manifest);
            }
        }
        // 返回组件清单索引。
        return result;
    }

    /**
     * 判断端口集合中是否存在指定端口。
     *
     * @param ports 端口集合
     * @param portId 端口标识
     * @return 是否存在端口
     */
    private boolean hasPort(Collection<ComponentPort> ports, String portId) {
        // 判断端口集合是否为空。
        if (ports == null) {
            // 空端口集合视为不存在端口。
            return false;
        }
        // 遍历端口集合。
        for (ComponentPort port : ports) {
            // 判断端口标识是否匹配。
            if (port.getId().equals(portId)) {
                // 命中端口。
                return true;
            }
        }
        // 未命中端口。
        return false;
    }

    /**
     * 构建组件清单键。
     *
     * @param type 组件类型
     * @param version 组件版本
     * @return 组件清单键
     */
    public static String manifestKey(String type, String version) {
        // 拼接组件类型与组件版本。
        return type + "@" + (isBlank(version) ? "1.0.0" : version);
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 字符串值
     * @return 是否为空白
     */
    private static boolean isBlank(String value) {
        // 使用通用工具判断字符串是否为空白。
        return StringUtils.isBlank(value);
    }
}
