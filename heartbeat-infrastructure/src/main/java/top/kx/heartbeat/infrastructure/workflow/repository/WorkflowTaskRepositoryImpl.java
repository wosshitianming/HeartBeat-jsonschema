// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class WorkflowTaskRepositoryImpl implements WorkflowTaskRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WfProcessInstanceDOMapper instanceDOMapper;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WfTaskDOMapper taskDOMapper;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WfTaskActionDOMapper actionDOMapper;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ReliableWorkflowEventService eventService;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listTodoTasks(String assigneeId) {
        // 注释：设置或计算当前变量值。
        WfTaskDOExample example = new WfTaskDOExample();
        // 注释：执行当前代码行。
        example.createCriteria()
                // 注释：继续当前链式调用。
                .andTenantIdEqualTo(tenantId())
                // 注释：继续当前链式调用。
                .andStatusEqualTo(WorkflowTaskStatus.TODO.getCode())
                // 注释：继续当前链式调用。
                .andAssigneeIdEqualTo(longValue(assigneeId, 1L));
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC");
        // 注释：返回当前处理结果。
        return taskDOMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toTaskMap)
                // 注释：继续当前链式调用。
                .map(DomainRecord::of)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord completeTask(String taskId, String action, String operatorId, String comment) {
        // 注释：设置或计算当前变量值。
        WfTaskDO task = taskDOMapper.selectByPrimaryKey(longValue(taskId, -1L));
        // 注释：判断当前业务条件。
        if (task == null || !task.getTenantId().equals(tenantId())) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Workflow task not found: " + taskId);
            // 注释：结束当前代码块。
        }
        // 注释：判断当前业务条件。
        if (!WorkflowTaskStatus.TODO.matches(task.getStatus())) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Workflow task has been handled: " + taskId);
            // 注释：结束当前代码块。
        }

        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        WorkflowTaskAction workflowAction = WorkflowTaskAction.fromCode(action);
        // 注释：执行当前代码行。
        task.setStatus(workflowAction.taskStatusCode());
        // 注释：执行当前代码行。
        task.setComment(comment);
        // 注释：执行当前代码行。
        task.setCompletedAt(now);
        // 注释：执行当前代码行。
        taskDOMapper.updateByPrimaryKeySelective(task);

        // 注释：设置或计算当前变量值。
        WfTaskActionDO taskAction = new WfTaskActionDO();
        // 注释：执行当前代码行。
        taskAction.setTenantId(task.getTenantId());
        // 注释：执行当前代码行。
        taskAction.setTaskId(task.getId());
        // 注释：执行当前代码行。
        taskAction.setAction(action);
        // 注释：执行当前代码行。
        taskAction.setOperatorId(longValue(operatorId, 1L));
        // 注释：执行当前代码行。
        taskAction.setComment(comment);
        // 注释：执行当前代码行。
        taskAction.setCreateTime(now);
        // 注释：执行当前代码行。
        actionDOMapper.insertSelective(taskAction);

        // 注释：设置或计算当前变量值。
        WfProcessInstanceDO instance = instanceDOMapper.selectByPrimaryKey(task.getInstanceId());
        // 注释：执行当前代码行。
        instance.setStatus(workflowAction.instanceStatusCode());
        // 注释：执行当前代码行。
        instance.setEndedAt(now);
        // 注释：执行当前代码行。
        instance.setUpdateTime(now);
        // 注释：执行当前代码行。
        instanceDOMapper.updateByPrimaryKeySelective(instance);

        // 注释：执行当前代码行。
        eventService.createOutbox(
                // 注释：执行当前代码行。
                "WORKFLOW_TASK_COMPLETED",
                // 注释：执行当前代码行。
                "WF_TASK",
                // 注释：执行当前代码行。
                String.valueOf(task.getId()),
                // 注释：执行当前代码行。
                "{\"taskId\":\"" + task.getId() + "\",\"action\":\"" + action + "\"}"
                // 注释：结束当前表达式。
        );

        // 注释：返回当前处理结果。
        return DomainRecord.of(toTaskMap(task));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> toTaskMap(WfTaskDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", String.valueOf(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("instanceId", String.valueOf(entity.getInstanceId()));
        // 注释：执行当前代码行。
        row.put("name", entity.getName());
        // 注释：执行当前代码行。
        row.put("assigneeId", String.valueOf(entity.getAssigneeId()));
        // 注释：执行当前代码行。
        row.put("status", entity.getStatus());
        // 注释：执行当前代码行。
        row.put("comment", entity.getComment());
        // 注释：执行当前代码行。
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        // 注释：执行当前代码行。
        row.put("completedAt", String.valueOf(entity.getCompletedAt()));
        // 注释：返回当前处理结果。
        return row;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private long longValue(String value, long defaultValue) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return value == null || value.trim().isEmpty() ? defaultValue : Long.parseLong(value.trim());
            // 注释：捕获并处理当前异常。
        } catch (NumberFormatException ignored) {
            // 注释：返回当前处理结果。
            return defaultValue;
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
