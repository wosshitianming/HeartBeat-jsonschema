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

/**
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
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

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @param assigneeId 业务记录标识。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param taskId 业务记录标识。
     * @param action 业务处理所需参数。
     * @param operatorId 业务记录标识。
     * @param comment 业务处理所需参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private long longValue(String value, long defaultValue) {
        try {
            return value == null || value.trim().isEmpty() ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
