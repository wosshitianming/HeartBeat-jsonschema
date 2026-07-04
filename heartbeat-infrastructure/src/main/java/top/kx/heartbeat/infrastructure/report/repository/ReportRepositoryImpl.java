package top.kx.heartbeat.infrastructure.report.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.port.ReportRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportDatasetDO;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportDatasetDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportQueryLogDO;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportTemplateDO;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportTemplateDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.ReportQueryMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.report.ReportDatasetDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.report.ReportQueryLogDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.report.ReportTemplateDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ReportRepositoryImpl implements ReportRepository {

    @Autowired
    private ReportDatasetDOMapper datasetDOMapper;

    @Autowired
    private ReportTemplateDOMapper templateDOMapper;

    @Autowired
    private ReportQueryLogDOMapper queryLogDOMapper;

    @Autowired
    private ReportQueryMapper queryMapper;

    @Override
    public List<DomainRecord> listDatasets() {
        ReportDatasetDOExample example = new ReportDatasetDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return datasetDOMapper.selectByExample(example)
                .stream()
                .map(this::toRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord saveDataset(Map<String, Object> command) {
        ReportDatasetDO record = new ReportDatasetDO();
        record.setTenantId(tenantId());
        record.setName(value(command, "name", "数据集"));
        record.setDatasetKey(value(command, "datasetKey", value(command, "dataset_key", "")));
        record.setQuerySql(value(command, "querySql", value(command, "query_sql", "")));
        record.setParamsJson(jsonValue(command.get("params")));
        record.setStatus(value(command, "status", "ACTIVE"));
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        datasetDOMapper.insertSelective(record);

        ReportDatasetDOExample example = new ReportDatasetDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andDatasetKeyEqualTo(record.getDatasetKey());
        List<ReportDatasetDO> datasets = datasetDOMapper.selectByExample(example);
        return toRecord(datasets.isEmpty() ? record : datasets.get(0));
    }

    @Override
    public List<DomainRecord> listTemplates() {
        ReportTemplateDOExample example = new ReportTemplateDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return templateDOMapper.selectByExample(example)
                .stream()
                .map(this::toTemplateRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord saveTemplate(Map<String, Object> command) {
        ReportTemplateDO record = new ReportTemplateDO();
        record.setTenantId(tenantId());
        record.setDatasetId(longValue(value(command, "datasetId", value(command, "dataset_id", "0")), 0L));
        record.setName(value(command, "name", "模板"));
        record.setTemplateKey(value(command, "templateKey", value(command, "template_key", "")));
        record.setTemplateJson(jsonValue(command.get("template")));
        record.setStatus(value(command, "status", "ACTIVE"));
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        templateDOMapper.insertSelective(record);
        return toTemplateRecord(record);
    }

    @Override
    public DomainRecord getDataset(String id) {
        ReportDatasetDO record = datasetDOMapper.selectByPrimaryKey(longValue(id, -1L));
        if (record != null && record.getTenantId().equals(tenantId())) {
            return toRecord(record);
        }
        ReportDatasetDOExample example = new ReportDatasetDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andDatasetKeyEqualTo(id);
        List<ReportDatasetDO> datasets = datasetDOMapper.selectByExample(example);
        if (datasets.isEmpty()) {
            throw new IllegalArgumentException("数据集不存在: " + id);
        }
        return toRecord(datasets.get(0));
    }

    @Override
    public List<DomainRecord> query(String sql, Map<String, Object> params, int limit) {
        List<Map<String, Object>> rows = queryMapper.executeReportQuery(sql, params, limit);

        ReportQueryLogDO log = new ReportQueryLogDO();
        log.setTenantId(tenantId());
        log.setDatasetId(0L);
        log.setParamsJson(jsonValue(params));
        log.setRowCount(rows.size());
        log.setStatus("SUCCESS");
        log.setCreateTime(new Date());
        queryLogDOMapper.insertSelective(log);

        return rows.stream().map(DomainRecord::of).collect(Collectors.toList());
    }

    private DomainRecord toRecord(ReportDatasetDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entity.getId()));
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        row.put("name", entity.getName());
        row.put("datasetKey", entity.getDatasetKey());
        row.put("status", entity.getStatus());
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        row.put("querySql", entity.getQuerySql());
        row.put("params", readJson(entity.getParamsJson()));
        return DomainRecord.of(row);
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

    private com.fasterxml.jackson.databind.JsonNode readJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 解析失败", ex);
        }
    }

    private String jsonValue(Object value) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 序列化失败", ex);
        }
    }

    private String value(Map<String, Object> command, String key, String defaultValue) {
        Object value = command.get(key);
        return StringUtils.isBlank(value == null ? null : String.valueOf(value)) ? defaultValue : String.valueOf(value).trim();
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
