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

/**
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class ReportDatasetRepositoryImpl implements ReportDatasetRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ReportDatasetDOMapper datasetDOMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
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

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param record 应用层业务记录。
     * @param request 公众号管理请求参数。
     */
    private void applyDataset(ReportDatasetDO record, ReportDatasetRequest request) {
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "Dataset")));
        record.setDatasetKey(defaultText(request.getDatasetKey(), defaultText(record.getDatasetKey(), "")));
        record.setQuerySql(defaultText(request.getQuerySql(), defaultText(record.getQuerySql(), "")));
        record.setParamsJson(request.getParams() == null ? defaultJson(record.getParamsJson()) : jsonValue(request.getParams()));
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private ReportDatasetDO findDatasetById(long id) {
        if (id <= 0) {
            return null;
        }
        ReportDatasetDO record = datasetDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
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
