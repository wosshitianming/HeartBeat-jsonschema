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

    @Resource
    private ObjectMapper objectMapper;

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
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        WorkflowStartRequest safeRequest = request == null ? new WorkflowStartRequest() : request;
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        WfProcessDefinitionDO definition = definitionDOMapper.selectByPrimaryKey(longValue(definitionId, -1L));
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (definition == null || !definition.getTenantId().equals(tenantId())) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Workflow definition not found: " + definitionId);
        }
        // 比对当前业务状态，决定是否进入该处理分支。
        if (!WorkflowDefinitionStatus.DEPLOYED.matches(definition.getStatus())) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Workflow definition is not deployed: " + definitionId);
        }
        // 计算当前分支的中间结果，供后续判断或组装使用。
        long tenantId = tenantId();
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();

        // 创建数据库记录对象，承载即将写入的业务字段。
        WfProcessInstanceDO instance = new WfProcessInstanceDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setTenantId(tenantId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setDefinitionId(definition.getId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setBusinessKey(defaultText(safeRequest.getBusinessKey(), ""));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setTitle(defaultText(safeRequest.getTitle(), definition.getName()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setInitiatorId(longValue(defaultText(safeRequest.getInitiatorId(), "1"), 1L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setStatus(WorkflowInstanceStatus.RUNNING.getCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setPayload(jsonValue(safeRequest.getPayload()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setStartedAt(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setCreateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        instanceDOMapper.insertSelective(instance);

        // 创建数据库记录对象，承载即将写入的业务字段。
        WfTaskDO task = new WfTaskDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setTenantId(tenantId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setInstanceId(instance.getId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setName(firstTaskName(definition));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setAssigneeId(longValue(firstNonBlank(safeRequest.getAssigneeId(), safeRequest.getApproverId(), firstTaskAssignee(definition)), 1L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setStatus(WorkflowTaskStatus.TODO.getCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setComment("");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        task.setCreateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        taskDOMapper.insertSelective(task);

        // 设置持久化字段，保证数据库记录具备完整业务属性。
        instance.setCurrentTaskId(task.getId());
        // 将当前业务变更写入持久化层，保持数据状态同步。
        instanceDOMapper.updateByPrimaryKeySelective(instance);

        // 返回已经完成封装的业务结果。
        return DomainRecord.of(toInstanceMap(instance));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listInstances() {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        WfProcessInstanceDOExample example = new WfProcessInstanceDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("create_time DESC, id DESC");
        // 返回已经完成封装的业务结果。
        return instanceDOMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::toInstanceMap)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(DomainRecord::of)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> toInstanceMap(WfProcessInstanceDO entity) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> row = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("id", String.valueOf(entity.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("definitionId", String.valueOf(entity.getDefinitionId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("businessKey", entity.getBusinessKey());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("title", entity.getTitle());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("initiatorId", String.valueOf(entity.getInitiatorId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("status", entity.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("currentTaskId", String.valueOf(entity.getCurrentTaskId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("payload", readJson(entity.getPayload()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("startedAt", String.valueOf(entity.getStartedAt()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("endedAt", String.valueOf(entity.getEndedAt()));
        // 返回已经完成封装的业务结果。
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
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
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
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
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
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (String value : values) {
            // 根据当前业务条件选择对应处理路径。
            if (StringUtils.isNotBlank(value)) {
                // 返回已经完成封装的业务结果。
                return value.trim();
            }
        }
        // 返回已经完成封装的业务结果。
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
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return StringUtils.isBlank(value) ? defaultValue : Long.parseLong(value.trim());
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
