package top.kx.heartbeat.application.workflow;


import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.workflow.pipeline.WorkflowDefinitionCommandPipeline;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.workflow.port.WorkflowRepository;
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
    public Map<String, Object> createDefinition(Map<String, Object> command) {
        definitionCommandPipeline.handle(command);
        return workflowRepository.createDefinition(command).toMap();
    }

    public List<Map<String, Object>> listDefinitions() {
        return maps(workflowRepository.listDefinitions());
    }

    public Map<String, Object> getDefinition(String id) {
        return workflowRepository.getDefinition(id).toMap();
    }

    @Transactional
    public Map<String, Object> deployDefinition(String id) {
        Map<String, Object> definition = workflowRepository.getDefinition(id).toMap();
        if (StringUtils.isEmpty(stringValue(definition.get("definitionKey")))) {
            throw new IllegalArgumentException("流程定义 key 不能为空");
        }
        return workflowRepository.deployDefinition(id).toMap();
    }

    @Transactional
    public Map<String, Object> startInstance(String definitionId, Map<String, Object> command) {
        return workflowRepository.startInstance(definitionId, command).toMap();
    }

    public List<Map<String, Object>> listTodoTasks() {
        return maps(workflowRepository.listTodoTasks(currentUserProvider.currentUserId()));
    }

    public List<Map<String, Object>> listInstances() {
        return maps(workflowRepository.listInstances());
    }

    @Transactional
    public Map<String, Object> approve(String taskId, Map<String, Object> command) {
        return workflowRepository.completeTask(
                taskId,
                WorkflowTaskAction.APPROVE.getCode(),
                currentUserProvider.currentUserId(),
                stringValue(command.get("comment"))
        ).toMap();
    }

    @Transactional
    public Map<String, Object> reject(String taskId, Map<String, Object> command) {
        return workflowRepository.completeTask(
                taskId,
                WorkflowTaskAction.REJECT.getCode(),
                currentUserProvider.currentUserId(),
                stringValue(command.get("comment"))
        ).toMap();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }
}
