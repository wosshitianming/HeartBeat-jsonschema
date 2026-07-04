package top.kx.heartbeat.infrastructure.workflow.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.workflow.port.WorkflowRepository;
import top.kx.heartbeat.domain.workflow.*;
import top.kx.heartbeat.infrastructure.event.ReliableWorkflowEventService;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessDefinitionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessDefinitionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessInstanceDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessInstanceDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfTaskActionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfTaskDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfTaskDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfProcessDefinitionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfProcessInstanceDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfTaskActionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfTaskDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class WorkflowRepositoryImpl implements WorkflowRepository {

    @Autowired
    private WfProcessDefinitionDOMapper definitionDOMapper;

    @Autowired
    private WfProcessInstanceDOMapper instanceDOMapper;

    @Autowired
    private WfTaskDOMapper taskDOMapper;

    @Autowired
    private WfTaskActionDOMapper actionDOMapper;

    @Autowired
    private ReliableWorkflowEventService eventService;

    @Override
    public DomainRecord createDefinition(Map<String, Object> command) {
        long tenantId = tenantId();
        Date now = new Date();
        WfProcessDefinitionDO entity = new WfProcessDefinitionDO();
        entity.setTenantId(tenantId);
        entity.setName(value(command, "name", "未命名流程"));
        entity.setDefinitionKey(value(command, "key", value(command, "definitionKey", "")));
        if (StringUtils.isEmpty(entity.getDefinitionKey())) {
            entity.setDefinitionKey("wf-" + System.nanoTime());
        }
        entity.setVersionNo(intValue(command.get("versionNo"), 1));
        entity.setFormSchema(jsonValue(command.get("formSchema")));
        entity.setStatus(WorkflowDefinitionStatus.DRAFT.getCode());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        definitionDOMapper.insertSelective(entity);
        return DomainRecord.of(toDefinitionMap(entity));
    }

    @Override
    public List<DomainRecord> listDefinitions() {
        WfProcessDefinitionDOExample example = new WfProcessDefinitionDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return definitionDOMapper.selectByExample(example)
                .stream()
                .map(this::toDefinitionMap)
                .map(DomainRecord::of)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord getDefinition(String id) {
        long tenantId = tenantId();
        WfProcessDefinitionDO entity = definitionDOMapper.selectByPrimaryKey(longValue(id, -1L));
        if (entity == null || !entity.getTenantId().equals(tenantId)) {
            WfProcessDefinitionDOExample example = new WfProcessDefinitionDOExample();
            example.createCriteria().andTenantIdEqualTo(tenantId).andDefinitionKeyEqualTo(id);
            List<WfProcessDefinitionDO> list = definitionDOMapper.selectByExample(example);
            if (list.isEmpty()) {
                throw new IllegalArgumentException("流程定义不存在: " + id);
            }
            entity = list.get(0);
        }
        return DomainRecord.of(toDefinitionMap(entity));
    }

    @Override
    public DomainRecord deployDefinition(String id) {
        WfProcessDefinitionDO entity = definitionDOMapper.selectByPrimaryKey(longValue(id, -1L));
        if (entity == null) {
            throw new IllegalArgumentException("流程定义不存在: " + id);
        }
        entity.setStatus(WorkflowDefinitionStatus.DEPLOYED.getCode());
        Date now = new Date();
        entity.setDeployedAt(now);
        entity.setUpdateTime(now);
        definitionDOMapper.updateByPrimaryKeySelective(entity);
        return DomainRecord.of(toDefinitionMap(entity));
    }

    @Override
    public DomainRecord startInstance(String definitionId, Map<String, Object> command) {
        WfProcessDefinitionDO definition = definitionDOMapper.selectByPrimaryKey(longValue(definitionId, -1L));
        if (definition == null) {
            throw new IllegalArgumentException("流程定义不存在: " + definitionId);
        }
        if (!WorkflowDefinitionStatus.DEPLOYED.matches(definition.getStatus())) {
            throw new IllegalArgumentException("流程定义未部署: " + definitionId);
        }
        long tenantId = tenantId();
        Date now = new Date();

        WfProcessInstanceDO instance = new WfProcessInstanceDO();
        instance.setTenantId(tenantId);
        instance.setDefinitionId(definition.getId());
        instance.setBusinessKey(value(command, "businessKey", ""));
        instance.setTitle(value(command, "title", definition.getName()));
        instance.setInitiatorId(longValue(value(command, "initiatorId", "1"), 1L));
        instance.setStatus(WorkflowInstanceStatus.RUNNING.getCode());
        instance.setPayload(jsonValue(command.get("payload")));
        instance.setStartedAt(now);
        instance.setCreateTime(now);
        instance.setUpdateTime(now);
        instanceDOMapper.insertSelective(instance);

        WfTaskDO task = new WfTaskDO();
        task.setTenantId(tenantId);
        task.setInstanceId(instance.getId());
        task.setName(firstTaskName(definition));
        task.setAssigneeId(longValue(value(command, "assigneeId", value(command, "approverId", firstTaskAssignee(definition))), 1L));
        task.setStatus(WorkflowTaskStatus.TODO.getCode());
        task.setComment("");
        task.setCreateTime(now);
        taskDOMapper.insertSelective(task);

        instance.setCurrentTaskId(task.getId());
        instanceDOMapper.updateByPrimaryKeySelective(instance);

        return DomainRecord.of(toInstanceMap(instance));
    }

    @Override
    public List<DomainRecord> listTodoTasks(String assigneeId) {
        WfTaskDOExample example = new WfTaskDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andStatusEqualTo(WorkflowTaskStatus.TODO.getCode())
                .andAssigneeIdEqualTo(longValue(assigneeId, 1L));
        example.setOrderByClause("create_time DESC");
        return taskDOMapper.selectByExample(example)
                .stream()
                .map(this::toTaskMap)
                .map(DomainRecord::of)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainRecord> listInstances() {
        WfProcessInstanceDOExample example = new WfProcessInstanceDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return instanceDOMapper.selectByExample(example)
                .stream()
                .map(this::toInstanceMap)
                .map(DomainRecord::of)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord completeTask(String taskId, String action, String operatorId, String comment) {
        WfTaskDO task = taskDOMapper.selectByPrimaryKey(longValue(taskId, -1L));
        if (task == null || !task.getTenantId().equals(tenantId())) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        if (!WorkflowTaskStatus.TODO.matches(task.getStatus())) {
            throw new IllegalArgumentException("任务已处理: " + taskId);
        }

        Date now = new Date();
        WorkflowTaskAction workflowAction = WorkflowTaskAction.fromCode(action);
        task.setStatus(workflowAction.taskStatusCode());
        task.setComment(comment);
        task.setCompletedAt(now);
        taskDOMapper.updateByPrimaryKeySelective(task);

        WfTaskActionDO taskAction = new WfTaskActionDO();
        taskAction.setTenantId(task.getTenantId());
        taskAction.setTaskId(task.getId());
        taskAction.setAction(action);
        taskAction.setOperatorId(longValue(operatorId, 1L));
        taskAction.setComment(comment);
        taskAction.setCreateTime(now);
        actionDOMapper.insertSelective(taskAction);

        WfProcessInstanceDO instance = instanceDOMapper.selectByPrimaryKey(task.getInstanceId());
        instance.setStatus(workflowAction.instanceStatusCode());
        instance.setEndedAt(now);
        instance.setUpdateTime(now);
        instanceDOMapper.updateByPrimaryKeySelective(instance);

        eventService.createOutbox(
                "WORKFLOW_TASK_COMPLETED",
                "WF_TASK",
                String.valueOf(task.getId()),
                "{\"taskId\":\"" + task.getId() + "\",\"action\":\"" + action + "\"}"
        );

        return DomainRecord.of(toTaskMap(task));
    }

    private Map<String, Object> toDefinitionMap(WfProcessDefinitionDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entity.getId()));
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        row.put("name", entity.getName());
        row.put("definitionKey", entity.getDefinitionKey());
        row.put("versionNo", entity.getVersionNo());
        row.put("formSchema", readJson(entity.getFormSchema()));
        row.put("status", entity.getStatus());
        row.put("deployedAt", String.valueOf(entity.getDeployedAt()));
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        return row;
    }

    private Map<String, Object> toInstanceMap(WfProcessInstanceDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entity.getId()));
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        row.put("definitionId", String.valueOf(entity.getDefinitionId()));
        row.put("businessKey", entity.getBusinessKey());
        row.put("title", entity.getTitle());
        row.put("initiatorId", String.valueOf(entity.getInitiatorId()));
        row.put("status", entity.getStatus());
        row.put("currentTaskId", String.valueOf(entity.getCurrentTaskId()));
        row.put("payload", readJson(entity.getPayload()));
        row.put("startedAt", String.valueOf(entity.getStartedAt()));
        row.put("endedAt", String.valueOf(entity.getEndedAt()));
        return row;
    }

    private Map<String, Object> toTaskMap(WfTaskDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entity.getId()));
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        row.put("instanceId", String.valueOf(entity.getInstanceId()));
        row.put("name", entity.getName());
        row.put("assigneeId", String.valueOf(entity.getAssigneeId()));
        row.put("status", entity.getStatus());
        row.put("comment", entity.getComment());
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        row.put("completedAt", String.valueOf(entity.getCompletedAt()));
        return row;
    }

    private com.fasterxml.jackson.databind.JsonNode readJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 解析失败", ex);
        }
    }

    private String jsonValue(Object value) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 序列化失败", ex);
        }
    }

    private String firstTaskName(WfProcessDefinitionDO definition) {
        com.fasterxml.jackson.databind.JsonNode tasks = readJson(definition.getFormSchema()).path("userTasks");
        return tasks.isArray() && tasks.size() > 0 && StringUtils.isNotEmpty(tasks.get(0).path("name").asText())
                ? tasks.get(0).path("name").asText()
                : "审批";
    }

    private String firstTaskAssignee(WfProcessDefinitionDO definition) {
        com.fasterxml.jackson.databind.JsonNode tasks = readJson(definition.getFormSchema()).path("userTasks");
        return tasks.isArray() && tasks.size() > 0 ? tasks.get(0).path("assigneeId").asText("1") : "1";
    }

    private String value(Map<String, Object> command, String key, String defaultValue) {
        Object value = command.get(key);
        return StringUtils.isBlank(value == null ? null : String.valueOf(value)) ? defaultValue : String.valueOf(value).trim();
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private long longValue(String value, long defaultValue) {
        try {
            return StringUtils.isBlank(value) ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
