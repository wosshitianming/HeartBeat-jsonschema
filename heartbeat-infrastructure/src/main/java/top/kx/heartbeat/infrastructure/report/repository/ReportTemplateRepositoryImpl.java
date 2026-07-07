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

/**
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class ReportTemplateRepositoryImpl implements ReportTemplateRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ReportTemplateDOMapper templateDOMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
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

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param record 应用层业务记录。
     * @param request 公众号管理请求参数。
     */
    private void applyTemplate(ReportTemplateDO record, ReportTemplateRequest request) {
        record.setDatasetId(longValue(defaultText(request.getDatasetId(), String.valueOf(record.getDatasetId())), 0L));
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "Template")));
        record.setTemplateKey(defaultText(request.getTemplateKey(), defaultText(record.getTemplateKey(), "")));
        record.setTemplateJson(request.getTemplate() == null ? defaultJson(record.getTemplateJson()) : jsonValue(request.getTemplate()));
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private ReportTemplateDO findTemplateById(long id) {
        if (id <= 0) {
            return null;
        }
        ReportTemplateDO record = templateDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
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
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String defaultJson(String value) {
        return StringUtils.isBlank(value) ? "{}" : value;
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
     * @param raw 业务处理所需参数。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
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

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
