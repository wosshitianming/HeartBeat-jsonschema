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
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        WfTaskDOExample example = new WfTaskDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .andTenantIdEqualTo(tenantId())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .andStatusEqualTo(WorkflowTaskStatus.TODO.getCode())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .andAssigneeIdEqualTo(longValue(assigneeId, 1L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("create_time DESC");
        // 返回已经完成封装的业务结果。
        return taskDOMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::toTaskMap)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(DomainRecord::of)
                // 使用流式转换批量映射数据，减少中间状态暴露。
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
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        WfTaskDO task = taskDOMapper.selectByPrimaryKey(longValue(taskId, -1L));
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (task == null || !task.getTenantId().equals(tenantId())) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Workflow task not found: " + taskId);
        }
        // 比对当前业务状态，决定是否进入该处理分支。
        if (!WorkflowTaskStatus.TODO.matches(task.getStatus())) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Workflow task has been handled: " + taskId);
        }

        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 组装工作流事件状态，保证可靠事件可以被后续消费。
        WorkflowTaskAction workflowAction = WorkflowTaskAction.fromCode(action);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setStatus(workflowAction.taskStatusCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setComment(comment);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setCompletedAt(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        taskDOMapper.updateByPrimaryKeySelective(task);

        // 创建数据库记录对象，承载即将写入的业务字段。
        WfTaskActionDO taskAction = new WfTaskActionDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        taskAction.setTenantId(task.getTenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        taskAction.setTaskId(task.getId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        taskAction.setAction(action);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        taskAction.setOperatorId(longValue(operatorId, 1L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        taskAction.setComment(comment);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        taskAction.setCreateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        actionDOMapper.insertSelective(taskAction);

        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        WfProcessInstanceDO instance = instanceDOMapper.selectByPrimaryKey(task.getInstanceId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setStatus(workflowAction.instanceStatusCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setEndedAt(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        instanceDOMapper.updateByPrimaryKeySelective(instance);

        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        eventService.createOutbox(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "WORKFLOW_TASK_COMPLETED",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "WF_TASK",
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                String.valueOf(task.getId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "{\"taskId\":\"" + task.getId() + "\",\"action\":\"" + action + "\"}"
        );

        // 返回已经完成封装的业务结果。
        return DomainRecord.of(toTaskMap(task));
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> toTaskMap(WfTaskDO entity) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> row = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("id", String.valueOf(entity.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("instanceId", String.valueOf(entity.getInstanceId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("name", entity.getName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("assigneeId", String.valueOf(entity.getAssigneeId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("status", entity.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("comment", entity.getComment());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("completedAt", String.valueOf(entity.getCompletedAt()));
        // 返回已经完成封装的业务结果。
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
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return value == null || value.trim().isEmpty() ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
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
