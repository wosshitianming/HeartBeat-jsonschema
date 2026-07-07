// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class ReportTemplateRepositoryImpl implements ReportTemplateRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ReportTemplateDOMapper templateDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listTemplates() {
        // 注释：设置或计算当前变量值。
        ReportTemplateDOExample example = new ReportTemplateDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return templateDOMapper.selectByExampleWithBLOBs(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toTemplateRecord)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord saveTemplate(ReportTemplateRequest request) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        ReportTemplateDO record = findTemplateById(longValue(request.getId(), -1L));
        // 注释：判断当前业务条件。
        if (record == null) {
            // 注释：设置或计算当前变量值。
            record = new ReportTemplateDO();
            // 注释：执行当前代码行。
            record.setTenantId(tenantId());
            // 注释：执行当前代码行。
            record.setCreateTime(now);
            // 注释：执行当前代码行。
            applyTemplate(record, request);
            // 注释：执行当前代码行。
            record.setUpdateTime(now);
            // 注释：执行当前代码行。
            templateDOMapper.insertSelective(record);
            // 注释：返回当前处理结果。
            return toTemplateRecord(record);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        applyTemplate(record, request);
        // 注释：执行当前代码行。
        record.setUpdateTime(now);
        // 注释：执行当前代码行。
        templateDOMapper.updateByPrimaryKeySelective(record);
        // 注释：返回当前处理结果。
        return toTemplateRecord(record);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void applyTemplate(ReportTemplateDO record, ReportTemplateRequest request) {
        // 注释：执行当前代码行。
        record.setDatasetId(longValue(defaultText(request.getDatasetId(), String.valueOf(record.getDatasetId())), 0L));
        // 注释：执行当前代码行。
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "Template")));
        // 注释：执行当前代码行。
        record.setTemplateKey(defaultText(request.getTemplateKey(), defaultText(record.getTemplateKey(), "")));
        // 注释：设置或计算当前变量值。
        record.setTemplateJson(request.getTemplate() == null ? defaultJson(record.getTemplateJson()) : jsonValue(request.getTemplate()));
        // 注释：执行当前代码行。
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private ReportTemplateDO findTemplateById(long id) {
        // 注释：判断当前业务条件。
        if (id <= 0) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        ReportTemplateDO record = templateDOMapper.selectByPrimaryKey(id);
        // 注释：返回当前处理结果。
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord toTemplateRecord(ReportTemplateDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", String.valueOf(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("datasetId", String.valueOf(entity.getDatasetId()));
        // 注释：执行当前代码行。
        row.put("name", entity.getName());
        // 注释：执行当前代码行。
        row.put("templateKey", entity.getTemplateKey());
        // 注释：执行当前代码行。
        row.put("template", readJson(entity.getTemplateJson()));
        // 注释：执行当前代码行。
        row.put("status", entity.getStatus());
        // 注释：执行当前代码行。
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        // 注释：执行当前代码行。
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        // 注释：返回当前处理结果。
        return DomainRecord.of(row);
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
    private String defaultJson(String value) {
        // 注释：返回当前处理结果。
        return StringUtils.isBlank(value) ? "{}" : value;
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
    private long longValue(Object raw, long defaultValue) {
        // 注释：判断当前业务条件。
        if (raw instanceof Number) {
            // 注释：返回当前处理结果。
            return ((Number) raw).longValue();
            // 注释：结束当前代码块。
        }
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
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
    private Long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
