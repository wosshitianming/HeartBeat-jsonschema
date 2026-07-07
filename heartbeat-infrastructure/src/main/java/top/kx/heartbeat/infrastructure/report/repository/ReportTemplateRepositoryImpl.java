package top.kx.heartbeat.infrastructure.report.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.port.ReportTemplateRepository;
import top.kx.heartbeat.application.report.request.ReportTemplateRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportTemplateDO;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportTemplateDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.report.ReportTemplateDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ReportTemplateRepositoryImpl implements ReportTemplateRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ReportTemplateDOMapper templateDOMapper;

    @Override
    public List<DomainRecord> listTemplates() {
        ReportTemplateDOExample example = new ReportTemplateDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return templateDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toTemplateRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord saveTemplate(ReportTemplateRequest request) {
        Date now = new Date();
        ReportTemplateDO record = findTemplateById(longValue(request.getId(), -1L));
        if (record == null) {
            record = new ReportTemplateDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            applyTemplate(record, request);
            record.setUpdateTime(now);
            templateDOMapper.insertSelective(record);
            return toTemplateRecord(record);
        }
        applyTemplate(record, request);
        record.setUpdateTime(now);
        templateDOMapper.updateByPrimaryKeySelective(record);
        return toTemplateRecord(record);
    }

    private void applyTemplate(ReportTemplateDO record, ReportTemplateRequest request) {
        record.setDatasetId(longValue(defaultText(request.getDatasetId(), String.valueOf(record.getDatasetId())), 0L));
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "Template")));
        record.setTemplateKey(defaultText(request.getTemplateKey(), defaultText(record.getTemplateKey(), "")));
        record.setTemplateJson(request.getTemplate() == null ? defaultJson(record.getTemplateJson()) : jsonValue(request.getTemplate()));
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
    }

    private ReportTemplateDO findTemplateById(long id) {
        if (id <= 0) {
            return null;
        }
        ReportTemplateDO record = templateDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    private DomainRecord toTemplateRecord(ReportTemplateDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entity.getId()));
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        row.put("datasetId", String.valueOf(entity.getDatasetId()));
        row.put("name", entity.getName());
        row.put("templateKey", entity.getTemplateKey());
        row.put("template", readJson(entity.getTemplateJson()));
        row.put("status", entity.getStatus());
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        return DomainRecord.of(row);
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

    private String defaultJson(String value) {
        return StringUtils.isBlank(value) ? "{}" : value;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    private long longValue(Object raw, long defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        try {
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
