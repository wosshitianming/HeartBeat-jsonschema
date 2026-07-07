package top.kx.heartbeat.infrastructure.workflow.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.workflow.port.WorkflowTaskRepository;
import top.kx.heartbeat.domain.workflow.WorkflowTaskAction;
import top.kx.heartbeat.domain.workflow.WorkflowTaskStatus;
import top.kx.heartbeat.infrastructure.event.ReliableWorkflowEventService;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessInstanceDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfTaskActionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfTaskDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfTaskDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfProcessInstanceDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfTaskActionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfTaskDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class WorkflowTaskRepositoryImpl implements WorkflowTaskRepository {

    @Resource
    private WfProcessInstanceDOMapper instanceDOMapper;

    @Resource
    private WfTaskDOMapper taskDOMapper;

    @Resource
    private WfTaskActionDOMapper actionDOMapper;

    @Resource
    private ReliableWorkflowEventService eventService;

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
    public DomainRecord completeTask(String taskId, String action, String operatorId, String comment) {
        WfTaskDO task = taskDOMapper.selectByPrimaryKey(longValue(taskId, -1L));
        if (task == null || !task.getTenantId().equals(tenantId())) {
            throw new IllegalArgumentException("Workflow task not found: " + taskId);
        }
        if (!WorkflowTaskStatus.TODO.matches(task.getStatus())) {
            throw new IllegalArgumentException("Workflow task has been handled: " + taskId);
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

    private long longValue(String value, long defaultValue) {
        try {
            return value == null || value.trim().isEmpty() ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
