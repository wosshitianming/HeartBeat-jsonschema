package top.kx.heartbeat.application.workflow.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BPMN 工作流定义命令处理器。
 *
 * <p>负责把 BPMN XML 解析成系统内部表单结构。</p>
 */
@Component
public class BpmnWorkflowDefinitionCommandHandler implements WorkflowDefinitionCommandHandler {

    /**
     * 返回处理器执行顺序。
     *
     * @return 最高优先级
     */
    @Override
    public int getOrder() {
        // 返回最高优先级，确保 BPMN 先被规范化。
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 处理工作流定义命令上下文。
     *
     * @param context 工作流定义命令上下文
     */
    @Override
    public void handle(WorkflowDefinitionCommandContext context) {
        // 读取 BPMN XML 字符串。
        String bpmnXml = context.stringValue("bpmnXml");
        // 判断是否需要处理 BPMN XML。
        if (StringUtils.isEmpty(bpmnXml)) {
            // 没有 BPMN XML 时直接交给后续处理器。
            return;
        }
        // 解析 BPMN XML 并补齐命令参数。
        normalize(context, bpmnXml);
    }

    /**
     * 解析 BPMN XML 并写入命令参数。
     *
     * @param context 工作流定义命令上下文
     * @param bpmnXml BPMN XML 字符串
     */
    private void normalize(WorkflowDefinitionCommandContext context, String bpmnXml) {
        // 捕获 XML 解析异常并转换为业务参数异常。
        try {
            // 创建 XML 文档构建工厂。
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 禁用命名空间感知以兼容现有 BPMN 解析方式。
            factory.setNamespaceAware(false);
            // 解析 BPMN XML 文档。
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));
            // 读取 BPMN process 元素。
            Element process = firstElement(document, "process");
            // 判断 process 元素是否存在。
            if (process != null) {
                // 使用 process id 补齐定义编码。
                context.putIfBlank("definitionKey", process.getAttribute("id"));
                // 使用 process name 补齐定义名称。
                context.putIfBlank("name", process.getAttribute("name"));
            }
            // 写入内部表单结构。
            context.put("formSchema", formSchema(document, bpmnXml));
        } catch (Exception ex) {
            // 抛出统一 BPMN 解析失败异常。
            throw new IllegalArgumentException("BPMN XML 解析失败", ex);
        }
    }

    /**
     * 生成工作流表单结构。
     *
     * @param document BPMN XML 文档
     * @param bpmnXml BPMN XML 字符串
     * @return 工作流表单结构
     */
    private Map<String, Object> formSchema(Document document, String bpmnXml) {
        // 创建有序表单结构。
        Map<String, Object> formSchema = new LinkedHashMap<>();
        // 写入表单来源。
        formSchema.put("source", "BPMN");
        // 写入原始 BPMN XML。
        formSchema.put("bpmnXml", bpmnXml);
        // 写入用户任务列表。
        formSchema.put("userTasks", userTasks(document));
        // 返回表单结构。
        return formSchema;
    }

    /**
     * 提取 BPMN 用户任务列表。
     *
     * @param document BPMN XML 文档
     * @return 用户任务列表
     */
    private List<Map<String, Object>> userTasks(Document document) {
        // 创建用户任务结果列表。
        List<Map<String, Object>> userTasks = new ArrayList<>();
        // 读取全部 userTask 节点。
        NodeList tasks = document.getElementsByTagName("userTask");
        // 遍历 userTask 节点。
        for (int index = 0; index < tasks.getLength(); index++) {
            // 读取当前 userTask 元素。
            Element task = (Element) tasks.item(index);
            // 创建单个用户任务记录。
            Map<String, Object> item = new LinkedHashMap<>();
            // 写入用户任务标识。
            item.put("id", task.getAttribute("id"));
            // 写入用户任务名称。
            item.put("name", task.getAttribute("name"));
            // 写入用户任务处理人。
            item.put("assigneeId", firstNonBlank(task.getAttribute("heartbeat:assignee"), task.getAttribute("flowable:assignee"), task.getAttribute("activiti:assignee"), task.getAttribute("assignee")));
            // 加入用户任务结果列表。
            userTasks.add(item);
        }
        // 返回用户任务列表。
        return userTasks;
    }

    /**
     * 返回第一个指定标签元素。
     *
     * @param document XML 文档
     * @param name 标签名称
     * @return 第一个匹配元素
     */
    private Element firstElement(Document document, String name) {
        // 读取指定标签节点列表。
        NodeList nodes = document.getElementsByTagName(name);
        // 返回第一个节点或空值。
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    /**
     * 返回第一个非空字符串。
     *
     * @param values 候选字符串数组
     * @return 第一个非空字符串
     */
    private String firstNonBlank(String... values) {
        // 遍历候选字符串。
        for (String value : values) {
            // 判断当前字符串是否非空。
            if (StringUtils.isNotBlank(value)) {
                // 返回裁剪后的字符串。
                return value.trim();
            }
        }
        // 全部为空时返回空字符串。
        return "";
    }

}
