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
        Map<String, Object> definition = definitionRepository.getDefinition(id).toMap();
        if (StringUtils.isEmpty(stringValue(definition.get("definitionKey")))) {
            throw new IllegalArgumentException("Workflow definition key cannot be blank");
        }
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
        if (request == null || StringUtils.isBlank(request.getBpmnXml())) {
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(request.getBpmnXml().getBytes(StandardCharsets.UTF_8)));
            Element process = firstElement(document, "process");
            if (process != null) {
                if (StringUtils.isBlank(request.getDefinitionKey())) {
                    request.setDefinitionKey(process.getAttribute("id"));
                }
                if (StringUtils.isBlank(request.getName())) {
                    request.setName(process.getAttribute("name"));
                }
            }
            request.setFormSchema(formSchema(document, request.getBpmnXml()));
        } catch (Exception ex) {
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
        Map<String, Object> formSchema = new LinkedHashMap<>();
        formSchema.put("source", "BPMN");
        formSchema.put("bpmnXml", bpmnXml);
        formSchema.put("userTasks", userTasks(document));
        return formSchema;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调工作流相关仓储和领域规则。
     *
     * @param document 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> userTasks(Document document) {
        List<Map<String, Object>> userTasks = new ArrayList<>();
        NodeList tasks = document.getElementsByTagName("userTask");
        for (int index = 0; index < tasks.getLength(); index++) {
            Element task = (Element) tasks.item(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getAttribute("id"));
            item.put("name", task.getAttribute("name"));
            item.put("assigneeId", firstNonBlank(
                    task.getAttribute("heartbeat:assignee"),
                    task.getAttribute("flowable:assignee"),
                    task.getAttribute("activiti:assignee"),
                    task.getAttribute("assignee")
            ));
            userTasks.add(item);
        }
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
        NodeList nodes = document.getElementsByTagName(name);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调工作流相关仓储和领域规则。
     *
     * @param values 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
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
