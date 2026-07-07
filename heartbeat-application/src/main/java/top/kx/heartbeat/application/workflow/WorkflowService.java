// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Service
public class WorkflowService {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WorkflowDefinitionRepository definitionRepository;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WorkflowInstanceRepository instanceRepository;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WorkflowTaskRepository taskRepository;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private CurrentUserProvider currentUserProvider;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse createDefinition(WorkflowDefinitionRequest request) {
        // 注释：执行当前代码行。
        normalizeDefinition(request);
        // 注释：返回当前处理结果。
        return RecordResponse.from(definitionRepository.createDefinition(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listDefinitions() {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(definitionRepository.listDefinitions()));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse getDefinition(String id) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(definitionRepository.getDefinition(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse deployDefinition(String id) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> definition = definitionRepository.getDefinition(id).toMap();
        // 注释：判断当前业务条件。
        if (StringUtils.isEmpty(stringValue(definition.get("definitionKey")))) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Workflow definition key cannot be blank");
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return RecordResponse.from(definitionRepository.deployDefinition(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse startInstance(String definitionId, WorkflowStartRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(instanceRepository.startInstance(definitionId, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listTodoTasks() {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(taskRepository.listTodoTasks(currentUserProvider.currentUserId())));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listInstances() {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(instanceRepository.listInstances()));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse approve(String taskId, WorkflowTaskActionRequest request) {
        // 注释：返回当前处理结果。
        return complete(taskId, WorkflowTaskAction.APPROVE, request);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse reject(String taskId, WorkflowTaskActionRequest request) {
        // 注释：返回当前处理结果。
        return complete(taskId, WorkflowTaskAction.REJECT, request);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private RecordResponse complete(String taskId, WorkflowTaskAction action, WorkflowTaskActionRequest request) {
        // 注释：设置或计算当前变量值。
        String comment = request == null ? "" : stringValue(request.getComment());
        // 注释：返回当前处理结果。
        return RecordResponse.from(taskRepository.completeTask(
                // 注释：执行当前代码行。
                taskId,
                // 注释：执行当前代码行。
                action.getCode(),
                // 注释：执行当前代码行。
                currentUserProvider.currentUserId(),
                // 注释：执行当前代码行。
                comment
                // 注释：结束当前多行调用。
        ));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void normalizeDefinition(WorkflowDefinitionRequest request) {
        // 注释：判断当前业务条件。
        if (request == null || StringUtils.isBlank(request.getBpmnXml())) {
            // 注释：返回当前处理结果。
            return;
            // 注释：结束当前代码块。
        }
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：设置或计算当前变量值。
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 注释：执行当前代码行。
            factory.setNamespaceAware(false);
            // 注释：设置或计算当前变量值。
            Document document = factory.newDocumentBuilder()
                    // 注释：继续当前链式调用。
                    .parse(new ByteArrayInputStream(request.getBpmnXml().getBytes(StandardCharsets.UTF_8)));
            // 注释：设置或计算当前变量值。
            Element process = firstElement(document, "process");
            // 注释：判断当前业务条件。
            if (process != null) {
                // 注释：判断当前业务条件。
                if (StringUtils.isBlank(request.getDefinitionKey())) {
                    // 注释：执行当前代码行。
                    request.setDefinitionKey(process.getAttribute("id"));
                    // 注释：结束当前代码块。
                }
                // 注释：判断当前业务条件。
                if (StringUtils.isBlank(request.getName())) {
                    // 注释：执行当前代码行。
                    request.setName(process.getAttribute("name"));
                    // 注释：结束当前代码块。
                }
                // 注释：结束当前代码块。
            }
            // 注释：执行当前代码行。
            request.setFormSchema(formSchema(document, request.getBpmnXml()));
            // 注释：捕获并处理当前异常。
        } catch (Exception ex) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("BPMN XML parse failed", ex);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> formSchema(Document document, String bpmnXml) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> formSchema = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        formSchema.put("source", "BPMN");
        // 注释：执行当前代码行。
        formSchema.put("bpmnXml", bpmnXml);
        // 注释：执行当前代码行。
        formSchema.put("userTasks", userTasks(document));
        // 注释：返回当前处理结果。
        return formSchema;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> userTasks(Document document) {
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> userTasks = new ArrayList<>();
        // 注释：设置或计算当前变量值。
        NodeList tasks = document.getElementsByTagName("userTask");
        // 注释：遍历当前数据集合。
        for (int index = 0; index < tasks.getLength(); index++) {
            // 注释：设置或计算当前变量值。
            Element task = (Element) tasks.item(index);
            // 注释：设置或计算当前变量值。
            Map<String, Object> item = new LinkedHashMap<>();
            // 注释：执行当前代码行。
            item.put("id", task.getAttribute("id"));
            // 注释：执行当前代码行。
            item.put("name", task.getAttribute("name"));
            // 注释：执行当前代码行。
            item.put("assigneeId", firstNonBlank(
                    // 注释：执行当前代码行。
                    task.getAttribute("heartbeat:assignee"),
                    // 注释：执行当前代码行。
                    task.getAttribute("flowable:assignee"),
                    // 注释：执行当前代码行。
                    task.getAttribute("activiti:assignee"),
                    // 注释：执行当前代码行。
                    task.getAttribute("assignee")
                    // 注释：结束当前多行调用。
            ));
            // 注释：执行当前代码行。
            userTasks.add(item);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return userTasks;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Element firstElement(Document document, String name) {
        // 注释：设置或计算当前变量值。
        NodeList nodes = document.getElementsByTagName(name);
        // 注释：返回当前处理结果。
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String firstNonBlank(String... values) {
        // 注释：遍历当前数据集合。
        for (String value : values) {
            // 注释：判断当前业务条件。
            if (StringUtils.isNotBlank(value)) {
                // 注释：返回当前处理结果。
                return value.trim();
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return "";
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String stringValue(Object value) {
        // 注释：返回当前处理结果。
        return value == null ? "" : String.valueOf(value).trim();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 注释：返回当前处理结果。
        return records.stream().map(DomainRecord::toMap).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
