// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.workflow.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.workflow.port.WorkflowDefinitionRepository;
import top.kx.heartbeat.application.workflow.request.WorkflowDefinitionRequest;
import top.kx.heartbeat.domain.workflow.WorkflowDefinitionStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessDefinitionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessDefinitionDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfProcessDefinitionDOMapper;
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
public class WorkflowDefinitionRepositoryImpl implements WorkflowDefinitionRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WfProcessDefinitionDOMapper definitionDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createDefinition(WorkflowDefinitionRequest request) {
        // 注释：设置或计算当前变量值。
        WorkflowDefinitionRequest safeRequest = request == null ? new WorkflowDefinitionRequest() : request;
        // 注释：设置或计算当前变量值。
        long tenantId = tenantId();
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        WfProcessDefinitionDO entity = new WfProcessDefinitionDO();
        // 注释：执行当前代码行。
        entity.setTenantId(tenantId);
        // 注释：执行当前代码行。
        entity.setName(defaultText(safeRequest.getName(), "Workflow"));
        // 注释：执行当前代码行。
        entity.setDefinitionKey(defaultText(safeRequest.getDefinitionKey(), ""));
        // 注释：判断当前业务条件。
        if (StringUtils.isBlank(entity.getDefinitionKey())) {
            // 注释：执行当前代码行。
            entity.setDefinitionKey("wf-" + System.nanoTime());
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        entity.setVersionNo(safeRequest.getVersionNo() == null ? 1 : safeRequest.getVersionNo());
        // 注释：执行当前代码行。
        entity.setFormSchema(jsonValue(safeRequest.getFormSchema()));
        // 注释：执行当前代码行。
        entity.setStatus(WorkflowDefinitionStatus.DRAFT.getCode());
        // 注释：执行当前代码行。
        entity.setCreateTime(now);
        // 注释：执行当前代码行。
        entity.setUpdateTime(now);
        // 注释：执行当前代码行。
        definitionDOMapper.insertSelective(entity);
        // 注释：返回当前处理结果。
        return DomainRecord.of(toDefinitionMap(entity));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listDefinitions() {
        // 注释：设置或计算当前变量值。
        WfProcessDefinitionDOExample example = new WfProcessDefinitionDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return definitionDOMapper.selectByExampleWithBLOBs(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toDefinitionMap)
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
    public DomainRecord getDefinition(String id) {
        // 注释：返回当前处理结果。
        return DomainRecord.of(toDefinitionMap(requireDefinition(id)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord deployDefinition(String id) {
        // 注释：设置或计算当前变量值。
        WfProcessDefinitionDO entity = requireDefinition(id);
        // 注释：执行当前代码行。
        entity.setStatus(WorkflowDefinitionStatus.DEPLOYED.getCode());
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：执行当前代码行。
        entity.setDeployedAt(now);
        // 注释：执行当前代码行。
        entity.setUpdateTime(now);
        // 注释：执行当前代码行。
        definitionDOMapper.updateByPrimaryKeySelective(entity);
        // 注释：返回当前处理结果。
        return DomainRecord.of(toDefinitionMap(entity));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private WfProcessDefinitionDO requireDefinition(String id) {
        // 注释：设置或计算当前变量值。
        long tenantId = tenantId();
        // 注释：设置或计算当前变量值。
        WfProcessDefinitionDO entity = definitionDOMapper.selectByPrimaryKey(longValue(id, -1L));
        // 注释：判断当前业务条件。
        if (entity != null && entity.getTenantId().equals(tenantId)) {
            // 注释：返回当前处理结果。
            return entity;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        WfProcessDefinitionDOExample example = new WfProcessDefinitionDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId).andDefinitionKeyEqualTo(id);
        // 注释：设置或计算当前变量值。
        List<WfProcessDefinitionDO> list = definitionDOMapper.selectByExampleWithBLOBs(example);
        // 注释：判断当前业务条件。
        if (list.isEmpty()) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Workflow definition not found: " + id);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return list.get(0);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> toDefinitionMap(WfProcessDefinitionDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", String.valueOf(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("name", entity.getName());
        // 注释：执行当前代码行。
        row.put("definitionKey", entity.getDefinitionKey());
        // 注释：执行当前代码行。
        row.put("versionNo", entity.getVersionNo());
        // 注释：执行当前代码行。
        row.put("formSchema", readJson(entity.getFormSchema()));
        // 注释：执行当前代码行。
        row.put("status", entity.getStatus());
        // 注释：执行当前代码行。
        row.put("deployedAt", String.valueOf(entity.getDeployedAt()));
        // 注释：执行当前代码行。
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        // 注释：执行当前代码行。
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        // 注释：返回当前处理结果。
        return row;
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
