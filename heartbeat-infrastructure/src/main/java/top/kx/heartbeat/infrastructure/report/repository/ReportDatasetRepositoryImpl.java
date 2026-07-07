package top.kx.heartbeat.infrastructure.report.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.port.ReportDatasetRepository;
import top.kx.heartbeat.application.report.request.ReportDatasetRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportDatasetDO;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportDatasetDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.report.ReportDatasetDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ReportDatasetRepositoryImpl implements ReportDatasetRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ReportDatasetDOMapper datasetDOMapper;

    @Override
    public List<DomainRecord> listDatasets() {
        ReportDatasetDOExample example = new ReportDatasetDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return datasetDOMapper.selectByExample(example)
                .stream()
                .map(this::toDatasetRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord saveDataset(ReportDatasetRequest request) {
        Date now = new Date();
        ReportDatasetDO record = findDatasetById(longValue(request.getId(), -1L));
        if (record == null) {
            record = new ReportDatasetDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            applyDataset(record, request);
            record.setUpdateTime(now);
            datasetDOMapper.insertSelective(record);
            return toDatasetRecord(record);
        }
        applyDataset(record, request);
        record.setUpdateTime(now);
        datasetDOMapper.updateByPrimaryKeySelective(record);
        return toDatasetRecord(record);
    }

    @Override
    public DomainRecord getDataset(String id) {
        ReportDatasetDO record = findDatasetById(longValue(id, -1L));
        if (record != null) {
            return toDatasetRecord(record);
        }
        ReportDatasetDOExample example = new ReportDatasetDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andDatasetKeyEqualTo(id);
        List<ReportDatasetDO> datasets = datasetDOMapper.selectByExample(example);
        if (datasets.isEmpty()) {
            throw new IllegalArgumentException("Report dataset not found: " + id);
        }
        return toDatasetRecord(datasets.get(0));
    }

    private void applyDataset(ReportDatasetDO record, ReportDatasetRequest request) {
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "Dataset")));
        record.setDatasetKey(defaultText(request.getDatasetKey(), defaultText(record.getDatasetKey(), "")));
        record.setQuerySql(defaultText(request.getQuerySql(), defaultText(record.getQuerySql(), "")));
        record.setParamsJson(request.getParams() == null ? defaultJson(record.getParamsJson()) : jsonValue(request.getParams()));
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
    }

    private ReportDatasetDO findDatasetById(long id) {
        if (id <= 0) {
            return null;
        }
        ReportDatasetDO record = datasetDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    private DomainRecord toDatasetRecord(ReportDatasetDO entity) {
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
