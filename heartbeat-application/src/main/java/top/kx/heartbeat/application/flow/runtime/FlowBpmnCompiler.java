package top.kx.heartbeat.application.flow.runtime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.model.FlowEdge;
import top.kx.heartbeat.domain.flow.model.FlowNode;
import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;
import top.kx.heartbeat.domain.flow.validation.FlowDslValidator;
import top.kx.heartbeat.domain.flow.validation.FlowValidationIssue;
import top.kx.heartbeat.domain.flow.validation.FlowValidationResult;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flow DSL BPMN 编译器。
 *
 * <p>负责把 HeartBeat 可视化流程契约编译为 Flowable 可部署的 BPMN XML。</p>
 */
@Service
public class FlowBpmnCompiler {

    /**
     * BPMN XML 命名空间。
     */
    private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    /**
     * BPMN DI 命名空间。
     */
    private static final String BPMNDI_NS = "http://www.omg.org/spec/BPMN/20100524/DI";

    /**
     * OMG DI 命名空间。
     */
    private static final String OMGDI_NS = "http://www.omg.org/spec/DD/20100524/DI";

    /**
     * OMG DC 命名空间。
     */
    private static final String OMGDC_NS = "http://www.omg.org/spec/DD/20100524/DC";

    /**
     * XSI 命名空间。
     */
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Flowable 命名空间。
     */
    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";

    /**
     * Flow DSL 校验领域服务。
     */
    @Resource
    private FlowDslValidator validator;

    /**
     * 编译 Flow DSL 为 BPMN XML。
     *
     * @param flow 流程定义
     * @param manifests 节点组件清单
     * @return BPMN 编译结果
     */
    public FlowBpmnCompileResult compile(FlowDefinition flow, List<NodeComponentManifest> manifests) {
        // 执行领域层 DSL 校验。
        FlowValidationResult validation = validator.validate(flow, manifests);
        // 创建编译问题列表。
        List<FlowValidationIssue> issues = new ArrayList<>(validation.getIssues());
        // 执行编译期补充校验。
        validateCompileRules(flow, issues);
        // 判断是否存在编译问题。
        if (!issues.isEmpty()) {
            // 返回失败编译结果。
            return FlowBpmnCompileResult.invalid(issues);
        }
        // 构建组件清单索引。
        Map<String, NodeComponentManifest> manifestByNodeId = manifestsByNodeId(flow, manifests);
        // 解析流程定义键。
        String processKey = processDefinitionKey(flow);
        // 解析流程定义名称。
        String processName = StringUtils.defaultIfBlank(flow.getName(), processKey);
        // 创建 BPMN XML 构建器。
        StringBuilder xml = new StringBuilder();
        // 写入 XML 声明。
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        // 写入 definitions 开始标签。
        xml.append("<definitions xmlns=\"").append(BPMN_NS).append("\" ")
                .append("xmlns:bpmndi=\"").append(BPMNDI_NS).append("\" ")
                .append("xmlns:omgdi=\"").append(OMGDI_NS).append("\" ")
                .append("xmlns:omgdc=\"").append(OMGDC_NS).append("\" ")
                .append("xmlns:xsi=\"").append(XSI_NS).append("\" ")
                .append("xmlns:flowable=\"").append(FLOWABLE_NS).append("\" ")
                .append("targetNamespace=\"http://heartbeat.kx.top/flow\">\n");
        // 写入消息定义。
        appendMessages(xml, flow);
        // 写入 process 开始标签。
        xml.append("  <process id=\"").append(escape(processKey)).append("\" name=\"").append(escape(processName)).append("\" isExecutable=\"true\">\n");
        // 创建 BPMN 映射列表。
        List<FlowBpmnElementMapping> mappings = new ArrayList<>();
        // 写入全部 BPMN 节点。
        appendNodes(xml, flow, manifestByNodeId, mappings);
        // 写入全部 BPMN 连线。
        appendEdges(xml, flow);
        // 写入 process 结束标签。
        xml.append("  </process>\n");
        // 写入 definitions 结束标签。
        xml.append("</definitions>\n");
        // 创建编译结果。
        FlowBpmnCompileResult result = new FlowBpmnCompileResult();
        // 标记编译成功。
        result.setValid(true);
        // 写入流程定义键。
        result.setProcessDefinitionKey(processKey);
        // 写入流程定义名称。
        result.setProcessName(processName);
        // 写入 BPMN 资源名称。
        result.setResourceName(processKey + ".bpmn20.xml");
        // 写入 BPMN XML。
        result.setBpmnXml(xml.toString());
        // 写入 BPMN 摘要。
        result.setBpmnSha256(sha256(xml.toString()));
        // 写入映射列表。
        result.setMappings(mappings);
        // 返回编译结果。
        return result;
    }

    /**
     * 写入 BPMN 消息定义。
     *
     * @param xml XML 构建器
     * @param flow 流程定义
     */
    private void appendMessages(StringBuilder xml, FlowDefinition flow) {
        // 遍历流程节点。
        for (FlowNode node : flow.getNodes()) {
            // 判断是否为消息等待节点。
            if (!isWaitMessageNode(node)) {
                // 非消息等待节点跳过。
                continue;
            }
            // 解析消息名称。
            String messageName = String.valueOf(configValue(node, "messageName", "heartbeat_" + node.getId()));
            // 写入消息定义。
            xml.append("  <message id=\"").append(escape(safeId(messageName))).append("\" name=\"").append(escape(messageName)).append("\" />\n");
        }
    }

    /**
     * 写入 BPMN 节点。
     *
     * @param xml XML 构建器
     * @param flow 流程定义
     * @param manifestByNodeId 节点组件清单索引
     * @param mappings BPMN 映射列表
     */
    private void appendNodes(StringBuilder xml,
                             FlowDefinition flow,
                             Map<String, NodeComponentManifest> manifestByNodeId,
                             List<FlowBpmnElementMapping> mappings) {
        // 遍历流程节点。
        for (FlowNode node : flow.getNodes()) {
            // 解析 BPMN 元素标识。
            String elementId = nodeElementId(node);
            // 读取节点组件清单。
            NodeComponentManifest manifest = manifestByNodeId.get(node.getId());
            // 解析节点执行器标识。
            String executorId = executorId(manifest);
            // 写入节点映射。
            mappings.add(new FlowBpmnElementMapping(node.getId(), elementId, node.getType(), node.getVersion(), executorId));
            // 根据节点类型写入 BPMN 元素。
            appendNode(xml, node, elementId, executorId);
        }
    }

    /**
     * 写入单个 BPMN 节点。
     *
     * @param xml XML 构建器
     * @param node 流程节点
     * @param elementId BPMN 元素标识
     * @param executorId 节点执行器标识
     */
    private void appendNode(StringBuilder xml, FlowNode node, String elementId, String executorId) {
        // 判断是否为触发节点。
        if (isStartNode(node)) {
            // 写入开始事件。
            xml.append("    <startEvent id=\"").append(elementId).append("\" name=\"").append(escape(label(node))).append("\" />\n");
            // 结束当前节点写入。
            return;
        }
        // 判断是否为结束节点。
        if (isEndNode(node)) {
            // 写入结束事件。
            xml.append("    <endEvent id=\"").append(elementId).append("\" name=\"").append(escape(label(node))).append("\" />\n");
            // 结束当前节点写入。
            return;
        }
        // 判断是否为条件节点。
        if (isConditionNode(node)) {
            // 写入排他网关。
            xml.append("    <exclusiveGateway id=\"").append(elementId).append("\" name=\"").append(escape(label(node))).append("\" />\n");
            // 结束当前节点写入。
            return;
        }
        // 判断是否为用户任务节点。
        if (isUserTaskNode(node)) {
            // 写入用户任务。
            appendUserTask(xml, node, elementId);
            // 结束当前节点写入。
            return;
        }
        // 判断是否为消息等待节点。
        if (isWaitMessageNode(node)) {
            // 写入消息捕获事件。
            appendMessageCatchEvent(xml, node, elementId);
            // 结束当前节点写入。
            return;
        }
        // 判断是否为定时等待节点。
        if (isWaitTimerNode(node)) {
            // 写入定时捕获事件。
            appendTimerCatchEvent(xml, node, elementId);
            // 结束当前节点写入。
            return;
        }
        // 写入服务任务节点。
        appendServiceTask(xml, node, elementId, executorId);
    }

    /**
     * 写入服务任务。
     *
     * @param xml XML 构建器
     * @param node 流程节点
     * @param elementId BPMN 元素标识
     * @param executorId 节点执行器标识
     */
    private void appendServiceTask(StringBuilder xml, FlowNode node, String elementId, String executorId) {
        // 写入服务任务开始标签。
        xml.append("    <serviceTask id=\"").append(elementId).append("\" name=\"").append(escape(label(node))).append("\" flowable:delegateExpression=\"${flowableNodeDelegate}\">\n");
        // 写入节点标识扩展字段。
        xml.append("      <extensionElements>\n");
        // 写入 HeartBeat 节点标识字段。
        xml.append("        <flowable:field name=\"flowNodeId\"><flowable:string>").append(escape(node.getId())).append("</flowable:string></flowable:field>\n");
        // 写入 HeartBeat 节点类型字段。
        xml.append("        <flowable:field name=\"flowNodeType\"><flowable:string>").append(escape(node.getType())).append("</flowable:string></flowable:field>\n");
        // 写入 HeartBeat 执行器标识字段。
        xml.append("        <flowable:field name=\"flowExecutorId\"><flowable:string>").append(escape(executorId)).append("</flowable:string></flowable:field>\n");
        // 写入执行模式字段。
        xml.append("        <flowable:field name=\"runtimeMode\"><flowable:string>").append(isExternalIoNode(node) ? "EXTERNAL_IO" : "INLINE").append("</flowable:string></flowable:field>\n");
        // 写入扩展元素结束标签。
        xml.append("      </extensionElements>\n");
        // 写入服务任务结束标签。
        xml.append("    </serviceTask>\n");
    }

    /**
     * 写入用户任务。
     *
     * @param xml XML 构建器
     * @param node 流程节点
     * @param elementId BPMN 元素标识
     */
    private void appendUserTask(StringBuilder xml, FlowNode node, String elementId) {
        // 解析任务处理人。
        String assignee = String.valueOf(configValue(node, "assignee", configValue(node, "assigneeId", "")));
        // 写入用户任务。
        xml.append("    <userTask id=\"").append(elementId).append("\" name=\"").append(escape(label(node))).append("\" flowable:assignee=\"").append(escape(assignee)).append("\" />\n");
    }

    /**
     * 写入消息捕获事件。
     *
     * @param xml XML 构建器
     * @param node 流程节点
     * @param elementId BPMN 元素标识
     */
    private void appendMessageCatchEvent(StringBuilder xml, FlowNode node, String elementId) {
        // 解析消息名称。
        String messageName = String.valueOf(configValue(node, "messageName", "heartbeat_" + node.getId()));
        // 写入消息捕获事件开始标签。
        xml.append("    <intermediateCatchEvent id=\"").append(elementId).append("\" name=\"").append(escape(label(node))).append("\">\n");
        // 写入等待登记监听器。
        appendWaitRegistrationListener(xml);
        // 写入消息事件定义。
        xml.append("      <messageEventDefinition messageRef=\"").append(escape(safeId(messageName))).append("\" />\n");
        // 写入消息捕获事件结束标签。
        xml.append("    </intermediateCatchEvent>\n");
    }

    /**
     * 写入定时捕获事件。
     *
     * @param xml XML 构建器
     * @param node 流程节点
     * @param elementId BPMN 元素标识
     */
    private void appendTimerCatchEvent(StringBuilder xml, FlowNode node, String elementId) {
        // 解析定时表达式。
        String duration = String.valueOf(configValue(node, "duration", "PT1M"));
        // 写入定时捕获事件开始标签。
        xml.append("    <intermediateCatchEvent id=\"").append(elementId).append("\" name=\"").append(escape(label(node))).append("\">\n");
        // 写入等待登记监听器。
        appendWaitRegistrationListener(xml);
        // 写入定时事件定义开始标签。
        xml.append("      <timerEventDefinition>\n");
        // 写入持续时间表达式。
        xml.append("        <timeDuration>").append(escape(duration)).append("</timeDuration>\n");
        // 写入定时事件定义结束标签。
        xml.append("      </timerEventDefinition>\n");
        // 写入定时捕获事件结束标签。
        xml.append("    </intermediateCatchEvent>\n");
    }

    /**
     * 写入等待登记监听器。
     *
     * @param xml XML 构建器
     */
    private void appendWaitRegistrationListener(StringBuilder xml) {
        // 写入扩展元素开始标签。
        xml.append("      <extensionElements>\n");
        // 写入执行监听器。
        xml.append("        <flowable:executionListener event=\"start\" delegateExpression=\"${flowWaitRegistrationListener}\" />\n");
        // 写入扩展元素结束标签。
        xml.append("      </extensionElements>\n");
    }

    /**
     * 写入 BPMN 连线。
     *
     * @param xml XML 构建器
     * @param flow 流程定义
     */
    private void appendEdges(StringBuilder xml, FlowDefinition flow) {
        // 判断流程连线是否为空。
        if (flow.getEdges() == null) {
            // 空连线无需写入。
            return;
        }
        // 建立节点索引。
        Map<String, FlowNode> nodeById = nodeById(flow);
        // 遍历流程连线。
        for (FlowEdge edge : flow.getEdges()) {
            // 读取源节点。
            FlowNode source = nodeById.get(edge.getSource());
            // 读取目标节点。
            FlowNode target = nodeById.get(edge.getTarget());
            // 跳过无效连线。
            if (source == null || target == null) {
                // 继续处理下一条连线。
                continue;
            }
            // 解析连线标识。
            String edgeId = edgeElementId(edge);
            // 写入连线开始标签。
            xml.append("    <sequenceFlow id=\"").append(edgeId).append("\" sourceRef=\"").append(nodeElementId(source)).append("\" targetRef=\"").append(nodeElementId(target)).append("\">");
            // 判断是否需要写入条件表达式。
            if (isConditionNode(source)) {
                // 编码条件表达式。
                String encodedExpression = base64(String.valueOf(configValue(source, "expression", "")));
                // 写入受控条件表达式。
                xml.append("\n      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${flowConditionEvaluator.matchesEncoded(execution, '").append(escape(edge.getId())).append("', '").append(escape(edge.getSourcePort())).append("', '").append(encodedExpression).append("')}]]></conditionExpression>\n    ");
            }
            // 写入连线结束标签。
            xml.append("</sequenceFlow>\n");
        }
    }

    /**
     * 执行编译期规则校验。
     *
     * @param flow 流程定义
     * @param issues 编译问题列表
     */
    private void validateCompileRules(FlowDefinition flow, List<FlowValidationIssue> issues) {
        // 判断流程定义是否为空。
        if (flow == null || flow.getNodes() == null) {
            // 空流程无需继续校验。
            return;
        }
        // 创建结束节点标记。
        boolean hasEnd = false;
        // 遍历流程节点。
        for (FlowNode node : flow.getNodes()) {
            // 发现结束节点。
            hasEnd = hasEnd || isEndNode(node);
            // 校验节点配置中的明文密钥。
            validateSecretConfig(node, issues);
        }
        // 判断是否缺失结束节点。
        if (!hasEnd) {
            // 添加缺失结束节点问题。
            issues.add(new FlowValidationIssue("FLOW_END_REQUIRED", "$.nodes", "生产态流程必须包含结束节点"));
        }
    }

    /**
     * 校验节点配置中的明文密钥。
     *
     * @param node 流程节点
     * @param issues 编译问题列表
     */
    private void validateSecretConfig(FlowNode node, List<FlowValidationIssue> issues) {
        // 判断节点配置是否为空。
        if (node.getConfig() == null) {
            // 空配置无需校验。
            return;
        }
        // 遍历节点配置键。
        for (String key : node.getConfig().keySet()) {
            // 识别敏感配置键。
            if (StringUtils.containsIgnoreCase(key, "secret") || StringUtils.containsIgnoreCase(key, "password") || StringUtils.containsIgnoreCase(key, "token")) {
                // 添加敏感配置问题。
                issues.add(new FlowValidationIssue("FLOW_SECRET_IN_CONFIG", "$.nodes[" + node.getId() + "].config." + key, "流程 DSL 不允许保存明文敏感配置"));
            }
        }
    }

    /**
     * 按节点标识索引流程节点。
     *
     * @param flow 流程定义
     * @return 节点标识索引
     */
    private Map<String, FlowNode> nodeById(FlowDefinition flow) {
        // 创建节点索引。
        Map<String, FlowNode> result = new LinkedHashMap<>();
        // 遍历流程节点。
        for (FlowNode node : flow.getNodes()) {
            // 写入节点索引。
            result.put(node.getId(), node);
        }
        // 返回节点索引。
        return result;
    }

    /**
     * 按节点标识索引组件清单。
     *
     * @param flow 流程定义
     * @param manifests 组件清单列表
     * @return 节点标识到组件清单映射
     */
    private Map<String, NodeComponentManifest> manifestsByNodeId(FlowDefinition flow, List<NodeComponentManifest> manifests) {
        // 创建组件清单键索引。
        Map<String, NodeComponentManifest> byKey = new LinkedHashMap<>();
        // 遍历组件清单。
        for (NodeComponentManifest manifest : manifests) {
            // 写入组件清单键索引。
            byKey.put(FlowDslValidator.manifestKey(manifest.getType(), manifest.getVersion()), manifest);
        }
        // 创建节点组件清单索引。
        Map<String, NodeComponentManifest> result = new LinkedHashMap<>();
        // 遍历流程节点。
        for (FlowNode node : flow.getNodes()) {
            // 写入节点对应的组件清单。
            result.put(node.getId(), byKey.get(FlowDslValidator.manifestKey(node.getType(), node.getVersion())));
        }
        // 返回节点组件清单索引。
        return result;
    }

    /**
     * 解析执行器标识。
     *
     * @param manifest 节点组件清单
     * @return 执行器标识
     */
    private String executorId(NodeComponentManifest manifest) {
        // 判断组件清单是否为空。
        if (manifest == null || manifest.getRuntime() == null) {
            // 返回空执行器标识。
            return "";
        }
        // 返回组件运行时执行器标识。
        return StringUtils.defaultString(manifest.getRuntime().getExecutor());
    }

    /**
     * 判断是否为开始节点。
     *
     * @param node 流程节点
     * @return 是否为开始节点
     */
    private boolean isStartNode(FlowNode node) {
        // 判断节点类型是否为触发器。
        return StringUtils.startsWith(node.getType(), "trigger.") || StringUtils.startsWith(node.getType(), "builtin:trigger.");
    }

    /**
     * 判断是否为结束节点。
     *
     * @param node 流程节点
     * @return 是否为结束节点
     */
    private boolean isEndNode(FlowNode node) {
        // 判断节点类型是否为结束组件。
        return "system.end".equals(node.getType()) || "builtin:system.end".equals(node.getType());
    }

    /**
     * 判断是否为条件节点。
     *
     * @param node 流程节点
     * @return 是否为条件节点
     */
    private boolean isConditionNode(FlowNode node) {
        // 判断节点类型是否为条件组件。
        return "logic.condition".equals(node.getType()) || "builtin:logic.condition".equals(node.getType());
    }

    /**
     * 判断是否为用户任务节点。
     *
     * @param node 流程节点
     * @return 是否为用户任务节点
     */
    private boolean isUserTaskNode(FlowNode node) {
        // 判断节点类型是否包含用户任务语义。
        return StringUtils.containsIgnoreCase(node.getType(), "userTask") || StringUtils.containsIgnoreCase(node.getType(), "approval");
    }

    /**
     * 判断是否为消息等待节点。
     *
     * @param node 流程节点
     * @return 是否为消息等待节点
     */
    private boolean isWaitMessageNode(FlowNode node) {
        // 判断节点类型是否为消息等待组件。
        return StringUtils.containsIgnoreCase(node.getType(), "wait.message");
    }

    /**
     * 判断是否为定时等待节点。
     *
     * @param node 流程节点
     * @return 是否为定时等待节点
     */
    private boolean isWaitTimerNode(FlowNode node) {
        // 判断节点类型是否为定时等待组件。
        return StringUtils.containsIgnoreCase(node.getType(), "wait.timer");
    }

    /**
     * 判断是否为外部 I/O 节点。
     *
     * @param node 流程节点
     * @return 是否为外部 I/O 节点
     */
    private boolean isExternalIoNode(FlowNode node) {
        // 读取节点类型。
        String type = node.getType();
        // 判断节点类型是否为长耗时外部 I/O。
        return StringUtils.contains(type, ".http.") || StringUtils.contains(type, ".mysql.") || StringUtils.contains(type, ".redis.") || StringUtils.contains(type, ".mq.")
                || StringUtils.contains(type, ":http.") || StringUtils.contains(type, ":mysql.") || StringUtils.contains(type, ":redis.") || StringUtils.contains(type, ":mq.");
    }

    /**
     * 解析节点展示名称。
     *
     * @param node 流程节点
     * @return 节点展示名称
     */
    private String label(FlowNode node) {
        // 读取配置中的名称。
        Object label = configValue(node, "label", null);
        // 返回节点名称或类型兜底。
        return StringUtils.defaultIfBlank(label == null ? null : String.valueOf(label), node.getType());
    }

    /**
     * 安全读取节点配置值。
     *
     * @param node 流程节点
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    private Object configValue(FlowNode node, String key, Object defaultValue) {
        // 判断节点配置是否为空。
        if (node.getConfig() == null) {
            // 返回默认配置值。
            return defaultValue;
        }
        // 返回配置值或默认值。
        return node.getConfig().getOrDefault(key, defaultValue);
    }

    /**
     * 生成流程定义键。
     *
     * @param flow 流程定义
     * @return 流程定义键
     */
    private String processDefinitionKey(FlowDefinition flow) {
        // 优先使用流程编码。
        String raw = StringUtils.defaultIfBlank(flow.getCode(), "flow_" + flow.getId());
        // 返回安全流程定义键。
        return safeId(raw);
    }

    /**
     * 生成节点 BPMN 元素标识。
     *
     * @param node 流程节点
     * @return BPMN 元素标识
     */
    private String nodeElementId(FlowNode node) {
        // 返回节点元素标识。
        return "node_" + safeId(node.getId());
    }

    /**
     * 生成连线 BPMN 元素标识。
     *
     * @param edge 流程连线
     * @return BPMN 连线元素标识
     */
    private String edgeElementId(FlowEdge edge) {
        // 解析原始连线标识。
        String raw = StringUtils.defaultIfBlank(edge.getId(), edge.getSource() + "_" + edge.getTarget() + "_" + edge.getSourcePort());
        // 返回连线元素标识。
        return "edge_" + safeId(raw);
    }

    /**
     * 转换为安全 XML ID。
     *
     * @param value 原始值
     * @return 安全 XML ID
     */
    private String safeId(String value) {
        // 替换非法字符。
        String safe = StringUtils.defaultString(value).replaceAll("[^A-Za-z0-9_]", "_");
        // 判断是否需要补充前缀。
        if (safe.isEmpty() || !Character.isLetter(safe.charAt(0))) {
            // 补充字母前缀。
            return "flow_" + safe;
        }
        // 返回安全标识。
        return safe;
    }

    /**
     * 转义 XML 文本。
     *
     * @param value 原始文本
     * @return XML 安全文本
     */
    private String escape(String value) {
        // 处理空文本。
        String text = StringUtils.defaultString(value);
        // 执行 XML 转义。
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    /**
     * 计算 SHA-256 摘要。
     *
     * @param text 原始文本
     * @return SHA-256 十六进制摘要
     */
    private String sha256(String text) {
        try {
            // 创建 SHA-256 摘要器。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 计算字节摘要。
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            // 创建十六进制构建器。
            StringBuilder hex = new StringBuilder();
            // 遍历摘要字节。
            for (byte item : bytes) {
                // 写入两位十六进制。
                hex.append(String.format("%02x", item));
            }
            // 返回十六进制摘要。
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            // 抛出不可恢复的 JDK 环境异常。
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", ex);
        }
    }

    /**
     * Base64 编码文本。
     *
     * @param text 原始文本
     * @return Base64 编码文本
     */
    private String base64(String text) {
        // 执行 Base64 编码。
        return Base64.getEncoder().encodeToString(StringUtils.defaultString(text).getBytes(StandardCharsets.UTF_8));
    }
}
