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

    @Transactional
    public RecordResponse createDefinition(WorkflowDefinitionRequest request) {
        normalizeDefinition(request);
        return RecordResponse.from(definitionRepository.createDefinition(request));
    }

    public List<RecordResponse> listDefinitions() {
        return RecordResponse.fromMaps(maps(definitionRepository.listDefinitions()));
    }

    public RecordResponse getDefinition(String id) {
        return RecordResponse.from(definitionRepository.getDefinition(id));
    }

    @Transactional
    public RecordResponse deployDefinition(String id) {
        Map<String, Object> definition = definitionRepository.getDefinition(id).toMap();
        if (StringUtils.isEmpty(stringValue(definition.get("definitionKey")))) {
            throw new IllegalArgumentException("Workflow definition key cannot be blank");
        }
        return RecordResponse.from(definitionRepository.deployDefinition(id));
    }

    @Transactional
    public RecordResponse startInstance(String definitionId, WorkflowStartRequest request) {
        return RecordResponse.from(instanceRepository.startInstance(definitionId, request));
    }

    public List<RecordResponse> listTodoTasks() {
        return RecordResponse.fromMaps(maps(taskRepository.listTodoTasks(currentUserProvider.currentUserId())));
    }

    public List<RecordResponse> listInstances() {
        return RecordResponse.fromMaps(maps(instanceRepository.listInstances()));
    }

    @Transactional
    public RecordResponse approve(String taskId, WorkflowTaskActionRequest request) {
        return complete(taskId, WorkflowTaskAction.APPROVE, request);
    }

    @Transactional
    public RecordResponse reject(String taskId, WorkflowTaskActionRequest request) {
        return complete(taskId, WorkflowTaskAction.REJECT, request);
    }

    private RecordResponse complete(String taskId, WorkflowTaskAction action, WorkflowTaskActionRequest request) {
        String comment = request == null ? "" : stringValue(request.getComment());
        return RecordResponse.from(taskRepository.completeTask(
                taskId,
                action.getCode(),
                currentUserProvider.currentUserId(),
                comment
        ));
    }

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

    private Map<String, Object> formSchema(Document document, String bpmnXml) {
        Map<String, Object> formSchema = new LinkedHashMap<>();
        formSchema.put("source", "BPMN");
        formSchema.put("bpmnXml", bpmnXml);
        formSchema.put("userTasks", userTasks(document));
        return formSchema;
    }

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

    private Element firstElement(Document document, String name) {
        NodeList nodes = document.getElementsByTagName(name);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(Collectors.toList());
    }
}
