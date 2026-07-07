// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.workflow.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.workflow.port.WorkflowInstanceRepository;
import top.kx.heartbeat.application.workflow.request.WorkflowStartRequest;
import top.kx.heartbeat.domain.workflow.WorkflowDefinitionStatus;
import top.kx.heartbeat.domain.workflow.WorkflowInstanceStatus;
import top.kx.heartbeat.domain.workflow.WorkflowTaskStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessDefinitionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessInstanceDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessInstanceDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfTaskDO;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfProcessDefinitionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfProcessInstanceDOMapper;
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
public class WorkflowInstanceRepositoryImpl implements WorkflowInstanceRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WfProcessDefinitionDOMapper definitionDOMapper;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WfProcessInstanceDOMapper instanceDOMapper;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WfTaskDOMapper taskDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord startInstance(String definitionId, WorkflowStartRequest request) {
        // 注释：设置或计算当前变量值。
        WorkflowStartRequest safeRequest = request == null ? new WorkflowStartRequest() : request;
        // 注释：设置或计算当前变量值。
        WfProcessDefinitionDO definition = definitionDOMapper.selectByPrimaryKey(longValue(definitionId, -1L));
        // 注释：判断当前业务条件。
        if (definition == null || !definition.getTenantId().equals(tenantId())) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Workflow definition not found: " + definitionId);
            // 注释：结束当前代码块。
        }
        // 注释：判断当前业务条件。
        if (!WorkflowDefinitionStatus.DEPLOYED.matches(definition.getStatus())) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Workflow definition is not deployed: " + definitionId);
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        long tenantId = tenantId();
        // 注释：设置或计算当前变量值。
        Date now = new Date();

        // 注释：设置或计算当前变量值。
        WfProcessInstanceDO instance = new WfProcessInstanceDO();
        // 注释：执行当前代码行。
        instance.setTenantId(tenantId);
        // 注释：执行当前代码行。
        instance.setDefinitionId(definition.getId());
        // 注释：执行当前代码行。
        instance.setBusinessKey(defaultText(safeRequest.getBusinessKey(), ""));
        // 注释：执行当前代码行。
        instance.setTitle(defaultText(safeRequest.getTitle(), definition.getName()));
        // 注释：执行当前代码行。
        instance.setInitiatorId(longValue(defaultText(safeRequest.getInitiatorId(), "1"), 1L));
        // 注释：执行当前代码行。
        instance.setStatus(WorkflowInstanceStatus.RUNNING.getCode());
        // 注释：执行当前代码行。
        instance.setPayload(jsonValue(safeRequest.getPayload()));
        // 注释：执行当前代码行。
        instance.setStartedAt(now);
        // 注释：执行当前代码行。
        instance.setCreateTime(now);
        // 注释：执行当前代码行。
        instance.setUpdateTime(now);
        // 注释：执行当前代码行。
        instanceDOMapper.insertSelective(instance);

        // 注释：设置或计算当前变量值。
        WfTaskDO task = new WfTaskDO();
        // 注释：执行当前代码行。
        task.setTenantId(tenantId);
        // 注释：执行当前代码行。
        task.setInstanceId(instance.getId());
        // 注释：执行当前代码行。
        task.setName(firstTaskName(definition));
        // 注释：执行当前代码行。
        task.setAssigneeId(longValue(firstNonBlank(safeRequest.getAssigneeId(), safeRequest.getApproverId(), firstTaskAssignee(definition)), 1L));
        // 注释：执行当前代码行。
        task.setStatus(WorkflowTaskStatus.TODO.getCode());
        // 注释：执行当前代码行。
        task.setComment("");
        // 注释：执行当前代码行。
        task.setCreateTime(now);
        // 注释：执行当前代码行。
        taskDOMapper.insertSelective(task);

        // 注释：执行当前代码行。
        instance.setCurrentTaskId(task.getId());
        // 注释：执行当前代码行。
        instanceDOMapper.updateByPrimaryKeySelective(instance);

        // 注释：返回当前处理结果。
        return DomainRecord.of(toInstanceMap(instance));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listInstances() {
        // 注释：设置或计算当前变量值。
        WfProcessInstanceDOExample example = new WfProcessInstanceDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return instanceDOMapper.selectByExampleWithBLOBs(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toInstanceMap)
                // 注释：继续当前链式调用。
                .map(DomainRecord::of)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> toInstanceMap(WfProcessInstanceDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", String.valueOf(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("definitionId", String.valueOf(entity.getDefinitionId()));
        // 注释：执行当前代码行。
        row.put("businessKey", entity.getBusinessKey());
        // 注释：执行当前代码行。
        row.put("title", entity.getTitle());
        // 注释：执行当前代码行。
        row.put("initiatorId", String.valueOf(entity.getInitiatorId()));
        // 注释：执行当前代码行。
        row.put("status", entity.getStatus());
        // 注释：执行当前代码行。
        row.put("currentTaskId", String.valueOf(entity.getCurrentTaskId()));
        // 注释：执行当前代码行。
        row.put("payload", readJson(entity.getPayload()));
        // 注释：执行当前代码行。
        row.put("startedAt", String.valueOf(entity.getStartedAt()));
        // 注释：执行当前代码行。
        row.put("endedAt", String.valueOf(entity.getEndedAt()));
        // 注释：返回当前处理结果。
        return row;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String firstTaskName(WfProcessDefinitionDO definition) {
        // 注释：设置或计算当前变量值。
        JsonNode tasks = readJson(definition.getFormSchema()).path("userTasks");
        // 注释：返回当前处理结果。
        return tasks.isArray() && tasks.size() > 0 && StringUtils.isNotEmpty(tasks.get(0).path("name").asText())
                // 注释：执行当前代码行。
                ? tasks.get(0).path("name").asText()
                // 注释：执行当前代码行。
                : "Approve";
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String firstTaskAssignee(WfProcessDefinitionDO definition) {
        // 注释：设置或计算当前变量值。
        JsonNode tasks = readJson(definition.getFormSchema()).path("userTasks");
        // 注释：返回当前处理结果。
        return tasks.isArray() && tasks.size() > 0 ? tasks.get(0).path("assigneeId").asText("1") : "1";
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private JsonNode readJson(String json) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
            // 注释：捕获并处理当前异常。
        } catch (Exception ex) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("JSON parse failed", ex);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String jsonValue(Object value) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
            // 注释：捕获并处理当前异常。
        } catch (Exception ex) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("JSON serialize failed", ex);
            // 注释：结束当前代码块。
        }
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
    private String defaultText(String value, String defaultValue) {
        // 注释：返回当前处理结果。
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private long longValue(String value, long defaultValue) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return StringUtils.isBlank(value) ? defaultValue : Long.parseLong(value.trim());
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
