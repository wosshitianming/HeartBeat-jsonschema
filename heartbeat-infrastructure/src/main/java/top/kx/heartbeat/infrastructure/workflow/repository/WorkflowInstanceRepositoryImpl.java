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
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class WorkflowInstanceRepositoryImpl implements WorkflowInstanceRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private WfProcessDefinitionDOMapper definitionDOMapper;

    @Resource
    private WfProcessInstanceDOMapper instanceDOMapper;

    @Resource
    private WfTaskDOMapper taskDOMapper;

    /**
     * 推进流程状态流转，并保持业务动作语义清晰，通过 Mapper 完成公众号管理数据访问。
     *
     * @param definitionId 业务记录标识。
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord startInstance(String definitionId, WorkflowStartRequest request) {
        WorkflowStartRequest safeRequest = request == null ? new WorkflowStartRequest() : request;
        WfProcessDefinitionDO definition = definitionDOMapper.selectByPrimaryKey(longValue(definitionId, -1L));
        if (definition == null || !definition.getTenantId().equals(tenantId())) {
            throw new IllegalArgumentException("Workflow definition not found: " + definitionId);
        }
        if (!WorkflowDefinitionStatus.DEPLOYED.matches(definition.getStatus())) {
            throw new IllegalArgumentException("Workflow definition is not deployed: " + definitionId);
        }
        long tenantId = tenantId();
        Date now = new Date();

        WfProcessInstanceDO instance = new WfProcessInstanceDO();
        instance.setTenantId(tenantId);
        instance.setDefinitionId(definition.getId());
        instance.setBusinessKey(defaultText(safeRequest.getBusinessKey(), ""));
        instance.setTitle(defaultText(safeRequest.getTitle(), definition.getName()));
        instance.setInitiatorId(longValue(defaultText(safeRequest.getInitiatorId(), "1"), 1L));
        instance.setStatus(WorkflowInstanceStatus.RUNNING.getCode());
        instance.setPayload(jsonValue(safeRequest.getPayload()));
        instance.setStartedAt(now);
        instance.setCreateTime(now);
        instance.setUpdateTime(now);
        instanceDOMapper.insertSelective(instance);

        WfTaskDO task = new WfTaskDO();
        task.setTenantId(tenantId);
        task.setInstanceId(instance.getId());
        task.setName(firstTaskName(definition));
        task.setAssigneeId(longValue(firstNonBlank(safeRequest.getAssigneeId(), safeRequest.getApproverId(), firstTaskAssignee(definition)), 1L));
        task.setStatus(WorkflowTaskStatus.TODO.getCode());
        task.setComment("");
        task.setCreateTime(now);
        taskDOMapper.insertSelective(task);

        instance.setCurrentTaskId(task.getId());
        instanceDOMapper.updateByPrimaryKeySelective(instance);

        return DomainRecord.of(toInstanceMap(instance));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listInstances() {
        WfProcessInstanceDOExample example = new WfProcessInstanceDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return instanceDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toInstanceMap)
                .map(DomainRecord::of)
                .collect(Collectors.toList());
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param definition 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String firstTaskName(WfProcessDefinitionDO definition) {
        JsonNode tasks = readJson(definition.getFormSchema()).path("userTasks");
        return tasks.isArray() && tasks.size() > 0 && StringUtils.isNotEmpty(tasks.get(0).path("name").asText())
                ? tasks.get(0).path("name").asText()
                : "Approve";
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param definition 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String firstTaskAssignee(WfProcessDefinitionDO definition) {
        JsonNode tasks = readJson(definition.getFormSchema()).path("userTasks");
        return tasks.isArray() && tasks.size() > 0 ? tasks.get(0).path("assigneeId").asText("1") : "1";
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param json 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON parse failed", ex);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String jsonValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param values 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
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
            return StringUtils.isBlank(value) ? defaultValue : Long.parseLong(value.trim());
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
