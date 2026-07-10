package top.kx.heartbeat.application.workflow;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.workflow.port.WorkflowDefinitionRepository;
import top.kx.heartbeat.application.workflow.port.WorkflowInstanceRepository;
import top.kx.heartbeat.application.workflow.port.WorkflowTaskRepository;
import top.kx.heartbeat.application.workflow.request.WorkflowDefinitionRequest;
import top.kx.heartbeat.application.workflow.request.WorkflowStartRequest;
import top.kx.heartbeat.application.workflow.request.WorkflowTaskActionRequest;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;
import top.kx.heartbeat.domain.workflow.WorkflowTaskAction;

import javax.annotation.Resource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 编排工作流应用用例，承接接口层请求并协调仓储与领域能力。
 */
@Service
public class WorkflowService {

    @Resource
    private WorkflowDefinitionRepository definitionRepository;

    @Resource
    private WorkflowInstanceRepository instanceRepository;

    @Resource
    private WorkflowTaskRepository taskRepository;

    @Resource
    private CurrentUserProvider currentUserProvider;

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调工作流相关仓储和领域规则。
     *
     * @param request 工作流请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createDefinition(WorkflowDefinitionRequest request) {
        normalizeDefinition(request);
        return RecordResponse.from(definitionRepository.createDefinition(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调工作流相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listDefinitions() {
        return RecordResponse.fromMaps(maps(definitionRepository.listDefinitions()));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，协调工作流相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    public RecordResponse getDefinition(String id) {
        return RecordResponse.from(definitionRepository.getDefinition(id));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调工作流相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse deployDefinition(String id) {
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        Map<String, Object> definition = definitionRepository.getDefinition(id).toMap();
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isEmpty(stringValue(definition.get("definitionKey")))) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Workflow definition key cannot be blank");
        }
        // 返回已经完成封装的业务结果。
        return RecordResponse.from(definitionRepository.deployDefinition(id));
    }

    /**
     * 推进流程状态流转，并保持业务动作语义清晰，协调工作流相关仓储和领域规则。
     *
     * @param definitionId 业务记录标识。
     * @param request 工作流请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse startInstance(String definitionId, WorkflowStartRequest request) {
        return RecordResponse.from(instanceRepository.startInstance(definitionId, request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调工作流相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listTodoTasks() {
        return RecordResponse.fromMaps(maps(taskRepository.listTodoTasks(currentUserProvider.currentUserId())));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调工作流相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listInstances() {
        return RecordResponse.fromMaps(maps(instanceRepository.listInstances()));
    }

    /**
     * 推进流程状态流转，并保持业务动作语义清晰，协调工作流相关仓储和领域规则。
     *
     * @param taskId 业务记录标识。
     * @param request 工作流请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse approve(String taskId, WorkflowTaskActionRequest request) {
        return complete(taskId, WorkflowTaskAction.APPROVE, request);
    }

    /**
     * 推进流程状态流转，并保持业务动作语义清晰，协调工作流相关仓储和领域规则。
     *
     * @param taskId 业务记录标识。
     * @param request 工作流请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse reject(String taskId, WorkflowTaskActionRequest request) {
        return complete(taskId, WorkflowTaskAction.REJECT, request);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调工作流相关仓储和领域规则。
     *
     * @param taskId 业务记录标识。
     * @param action 业务处理所需参数。
     * @param request 工作流请求参数。
     * @return 处理后的业务结果。
     */
    private RecordResponse complete(String taskId, WorkflowTaskAction action, WorkflowTaskActionRequest request) {
        String comment = request == null ? "" : stringValue(request.getComment());
        return RecordResponse.from(taskRepository.completeTask(
                taskId,
                action.getCode(),
                currentUserProvider.currentUserId(),
                comment
        ));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调工作流相关仓储和领域规则。
     *
     * @param request 工作流请求参数。
     */
    private void normalizeDefinition(WorkflowDefinitionRequest request) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (request == null || StringUtils.isBlank(request.getBpmnXml())) {
            // 返回已经完成封装的业务结果。
            return;
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 解析流程定义 XML，提取后续页面和任务需要的结构信息。
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            // 解析流程定义 XML，提取后续页面和任务需要的结构信息。
            Document document = factory.newDocumentBuilder()
                    // 按签名算法处理字节数据，保证验签结果可重复计算。
                    .parse(new ByteArrayInputStream(request.getBpmnXml().getBytes(StandardCharsets.UTF_8)));
            // 解析流程定义 XML，提取后续页面和任务需要的结构信息。
            Element process = firstElement(document, "process");
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (process != null) {
                // 校验关键文本参数，防止无效输入继续向后流转。
                if (StringUtils.isBlank(request.getDefinitionKey())) {
                    // 设置持久化字段，保证数据库记录具备完整业务属性。
                    request.setDefinitionKey(process.getAttribute("id"));
                }
                // 校验关键文本参数，防止无效输入继续向后流转。
                if (StringUtils.isBlank(request.getName())) {
                    // 设置持久化字段，保证数据库记录具备完整业务属性。
                    request.setName(process.getAttribute("name"));
                }
            }
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            request.setFormSchema(formSchema(document, request.getBpmnXml()));
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("BPMN XML parse failed", ex);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调工作流相关仓储和领域规则。
     *
     * @param document 业务处理所需参数。
     * @param bpmnXml 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> formSchema(Document document, String bpmnXml) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> formSchema = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        formSchema.put("source", "BPMN");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        formSchema.put("bpmnXml", bpmnXml);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        formSchema.put("userTasks", userTasks(document));
        // 返回已经完成封装的业务结果。
        return formSchema;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调工作流相关仓储和领域规则。
     *
     * @param document 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> userTasks(Document document) {
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> userTasks = new ArrayList<>();
        // 解析流程定义 XML，提取后续页面和任务需要的结构信息。
        NodeList tasks = document.getElementsByTagNameNS("*", "userTask");
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (int index = 0; index < tasks.getLength(); index++) {
            // 解析流程定义 XML，提取后续页面和任务需要的结构信息。
            Element task = (Element) tasks.item(index);
            // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
            Map<String, Object> item = new LinkedHashMap<>();
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("id", task.getAttribute("id"));
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("name", task.getAttribute("name"));
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("assigneeId", firstNonBlank(
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    task.getAttribute("heartbeat:assignee"),
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    task.getAttribute("flowable:assignee"),
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    task.getAttribute("activiti:assignee"),
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    task.getAttribute("assignee")
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            ));
            // 加入当前处理结果，供后续批量返回或继续组装。
            userTasks.add(item);
        }
        // 返回已经完成封装的业务结果。
        return userTasks;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调工作流相关仓储和领域规则。
     *
     * @param document 业务处理所需参数。
     * @param name 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private Element firstElement(Document document, String name) {
        NodeList nodes = document.getElementsByTagNameNS("*", name);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    /**
     * 创建仅允许解析本地 XML 内容的 BPMN 文档工厂。
     *
     * @return 禁用外部实体与外部资源访问的文档工厂
     * @throws Exception 当前 XML 解析器不支持必要的安全配置时抛出
     */
    private DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调工作流相关仓储和领域规则。
     *
     * @param values 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String firstNonBlank(String... values) {
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (String value : values) {
            // 根据当前业务条件选择对应处理路径。
            if (StringUtils.isNotBlank(value)) {
                // 返回已经完成封装的业务结果。
                return value.trim();
            }
        }
        // 返回已经完成封装的业务结果。
        return "";
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，协调工作流相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调工作流相关仓储和领域规则。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(Collectors.toList());
    }
}
