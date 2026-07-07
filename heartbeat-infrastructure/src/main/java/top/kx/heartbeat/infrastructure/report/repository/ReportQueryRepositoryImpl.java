package top.kx.heartbeat.infrastructure.report.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.port.ReportQueryRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportQueryLogDO;
import top.kx.heartbeat.infrastructure.persistence.mapper.ReportQueryMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.report.ReportQueryLogDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ReportQueryRepositoryImpl implements ReportQueryRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ReportQueryMapper queryMapper;

    @Resource
    private ReportQueryLogDOMapper queryLogDOMapper;

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

    private String jsonValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
