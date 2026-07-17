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

    @Resource
    private ObjectMapper objectMapper;

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
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        List<Map<String, Object>> rows = queryMapper.executeReportQuery(sql, params, limit);

        // 创建数据库记录对象，承载即将写入的业务字段。
        ReportQueryLogDO log = new ReportQueryLogDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        log.setTenantId(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        log.setDatasetId(0L);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        log.setParamsJson(jsonValue(params));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        log.setRowCount(rows.size());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        log.setStatus("SUCCESS");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        log.setCreateTime(new Date());
        // 将当前业务变更写入持久化层，保持数据状态同步。
        queryLogDOMapper.insertSelective(log);

        // 返回已经完成封装的业务结果。
        return rows.stream().map(DomainRecord::of).collect(Collectors.toList());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String jsonValue(Object value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
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
