package top.kx.heartbeat.application.workflow;


import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.workflow.pipeline.WorkflowDefinitionCommandPipeline;
import top.kx.heartbeat.application.workflow.port.WorkflowRepository;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;
import top.kx.heartbeat.domain.workflow.WorkflowTaskAction;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowService {

    @Resource
    private WorkflowRepository workflowRepository;
    @Resource
    private CurrentUserProvider currentUserProvider;
    @Resource
    private WorkflowDefinitionCommandPipeline definitionCommandPipeline;

    @Transactional
    public RecordResponse createDefinition(Map<String, Object> command) {
        definitionCommandPipeline.handle(command);
        return RecordResponse.from(workflowRepository.createDefinition(command));
    }

    public List<RecordResponse> listDefinitions() {
        return RecordResponse.fromMaps(maps(workflowRepository.listDefinitions()));
    }

    public RecordResponse getDefinition(String id) {
        return RecordResponse.from(workflowRepository.getDefinition(id));
    }

    @Transactional
    public RecordResponse deployDefinition(String id) {
        Map<String, Object> definition = workflowRepository.getDefinition(id).toMap();
        if (StringUtils.isEmpty(stringValue(definition.get("definitionKey")))) {
            throw new IllegalArgumentException("流程定义 key 不能为空");
        }
        return RecordResponse.from(workflowRepository.deployDefinition(id));
    }

    @Transactional
    public RecordResponse startInstance(String definitionId, Map<String, Object> command) {
        return RecordResponse.from(workflowRepository.startInstance(definitionId, command));
    }

    public List<RecordResponse> listTodoTasks() {
        return RecordResponse.fromMaps(maps(workflowRepository.listTodoTasks(currentUserProvider.currentUserId())));
    }

    public List<RecordResponse> listInstances() {
        return RecordResponse.fromMaps(maps(workflowRepository.listInstances()));
    }

    @Transactional
    public RecordResponse approve(String taskId, Map<String, Object> command) {
        return RecordResponse.from(workflowRepository.completeTask(
                taskId,
                WorkflowTaskAction.APPROVE.getCode(),
                currentUserProvider.currentUserId(),
                stringValue(command.get("comment"))
        ));
    }

    @Transactional
    public RecordResponse reject(String taskId, Map<String, Object> command) {
        return RecordResponse.from(workflowRepository.completeTask(
                taskId,
                WorkflowTaskAction.REJECT.getCode(),
                currentUserProvider.currentUserId(),
                stringValue(command.get("comment"))
        ));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }
}
