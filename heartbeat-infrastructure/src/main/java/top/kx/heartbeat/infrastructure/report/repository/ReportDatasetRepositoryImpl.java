// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class ReportDatasetRepositoryImpl implements ReportDatasetRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ReportDatasetDOMapper datasetDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listDatasets() {
        // 注释：设置或计算当前变量值。
        ReportDatasetDOExample example = new ReportDatasetDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return datasetDOMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toDatasetRecord)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord saveDataset(ReportDatasetRequest request) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        ReportDatasetDO record = findDatasetById(longValue(request.getId(), -1L));
        // 注释：判断当前业务条件。
        if (record == null) {
            // 注释：设置或计算当前变量值。
            record = new ReportDatasetDO();
            // 注释：执行当前代码行。
            record.setTenantId(tenantId());
            // 注释：执行当前代码行。
            record.setCreateTime(now);
            // 注释：执行当前代码行。
            applyDataset(record, request);
            // 注释：执行当前代码行。
            record.setUpdateTime(now);
            // 注释：执行当前代码行。
            datasetDOMapper.insertSelective(record);
            // 注释：返回当前处理结果。
            return toDatasetRecord(record);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        applyDataset(record, request);
        // 注释：执行当前代码行。
        record.setUpdateTime(now);
        // 注释：执行当前代码行。
        datasetDOMapper.updateByPrimaryKeySelective(record);
        // 注释：返回当前处理结果。
        return toDatasetRecord(record);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord getDataset(String id) {
        // 注释：设置或计算当前变量值。
        ReportDatasetDO record = findDatasetById(longValue(id, -1L));
        // 注释：判断当前业务条件。
        if (record != null) {
            // 注释：返回当前处理结果。
            return toDatasetRecord(record);
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        ReportDatasetDOExample example = new ReportDatasetDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andDatasetKeyEqualTo(id);
        // 注释：设置或计算当前变量值。
        List<ReportDatasetDO> datasets = datasetDOMapper.selectByExample(example);
        // 注释：判断当前业务条件。
        if (datasets.isEmpty()) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Report dataset not found: " + id);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return toDatasetRecord(datasets.get(0));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void applyDataset(ReportDatasetDO record, ReportDatasetRequest request) {
        // 注释：执行当前代码行。
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "Dataset")));
        // 注释：执行当前代码行。
        record.setDatasetKey(defaultText(request.getDatasetKey(), defaultText(record.getDatasetKey(), "")));
        // 注释：执行当前代码行。
        record.setQuerySql(defaultText(request.getQuerySql(), defaultText(record.getQuerySql(), "")));
        // 注释：设置或计算当前变量值。
        record.setParamsJson(request.getParams() == null ? defaultJson(record.getParamsJson()) : jsonValue(request.getParams()));
        // 注释：执行当前代码行。
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private ReportDatasetDO findDatasetById(long id) {
        // 注释：判断当前业务条件。
        if (id <= 0) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        ReportDatasetDO record = datasetDOMapper.selectByPrimaryKey(id);
        // 注释：返回当前处理结果。
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord toDatasetRecord(ReportDatasetDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", String.valueOf(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("name", entity.getName());
        // 注释：执行当前代码行。
        row.put("datasetKey", entity.getDatasetKey());
        // 注释：执行当前代码行。
        row.put("status", entity.getStatus());
        // 注释：执行当前代码行。
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        // 注释：执行当前代码行。
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        // 注释：执行当前代码行。
        row.put("querySql", entity.getQuerySql());
        // 注释：执行当前代码行。
        row.put("params", readJson(entity.getParamsJson()));
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
