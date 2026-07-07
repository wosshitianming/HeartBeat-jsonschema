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

@Repository
public class WorkflowDefinitionRepositoryImpl implements WorkflowDefinitionRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private WfProcessDefinitionDOMapper definitionDOMapper;

    @Override
    public DomainRecord createDefinition(WorkflowDefinitionRequest request) {
        WorkflowDefinitionRequest safeRequest = request == null ? new WorkflowDefinitionRequest() : request;
        long tenantId = tenantId();
        Date now = new Date();
        WfProcessDefinitionDO entity = new WfProcessDefinitionDO();
        entity.setTenantId(tenantId);
        entity.setName(defaultText(safeRequest.getName(), "Workflow"));
        entity.setDefinitionKey(defaultText(safeRequest.getDefinitionKey(), ""));
        if (StringUtils.isBlank(entity.getDefinitionKey())) {
            entity.setDefinitionKey("wf-" + System.nanoTime());
        }
        entity.setVersionNo(safeRequest.getVersionNo() == null ? 1 : safeRequest.getVersionNo());
        entity.setFormSchema(jsonValue(safeRequest.getFormSchema()));
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
        return definitionDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toDefinitionMap)
                .map(DomainRecord::of)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord getDefinition(String id) {
        return DomainRecord.of(toDefinitionMap(requireDefinition(id)));
    }

    @Override
    public DomainRecord deployDefinition(String id) {
        WfProcessDefinitionDO entity = requireDefinition(id);
        entity.setStatus(WorkflowDefinitionStatus.DEPLOYED.getCode());
        Date now = new Date();
        entity.setDeployedAt(now);
        entity.setUpdateTime(now);
        definitionDOMapper.updateByPrimaryKeySelective(entity);
        return DomainRecord.of(toDefinitionMap(entity));
    }

    private WfProcessDefinitionDO requireDefinition(String id) {
        long tenantId = tenantId();
        WfProcessDefinitionDO entity = definitionDOMapper.selectByPrimaryKey(longValue(id, -1L));
        if (entity != null && entity.getTenantId().equals(tenantId)) {
            return entity;
        }
        WfProcessDefinitionDOExample example = new WfProcessDefinitionDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId).andDefinitionKeyEqualTo(id);
        List<WfProcessDefinitionDO> list = definitionDOMapper.selectByExampleWithBLOBs(example);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Workflow definition not found: " + id);
        }
        return list.get(0);
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

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON parse failed", ex);
        }
    }

    private String jsonValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
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
