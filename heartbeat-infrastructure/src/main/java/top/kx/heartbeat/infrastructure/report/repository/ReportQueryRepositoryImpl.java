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

/**
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class ReportQueryRepositoryImpl implements ReportQueryRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ReportQueryMapper queryMapper;

    @Resource
    private ReportQueryLogDOMapper queryLogDOMapper;

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param sql 业务处理所需参数。
     * @param params 业务处理所需参数。
     * @param limit 业务处理所需参数。
     * @return 处理后的业务结果。
     */
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
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
