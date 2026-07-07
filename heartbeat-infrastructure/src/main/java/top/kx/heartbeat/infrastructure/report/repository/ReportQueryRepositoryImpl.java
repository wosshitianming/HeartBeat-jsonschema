// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class ReportQueryRepositoryImpl implements ReportQueryRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ReportQueryMapper queryMapper;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ReportQueryLogDOMapper queryLogDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> query(String sql, Map<String, Object> params, int limit) {
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> rows = queryMapper.executeReportQuery(sql, params, limit);

        // 注释：设置或计算当前变量值。
        ReportQueryLogDO log = new ReportQueryLogDO();
        // 注释：执行当前代码行。
        log.setTenantId(tenantId());
        // 注释：执行当前代码行。
        log.setDatasetId(0L);
        // 注释：执行当前代码行。
        log.setParamsJson(jsonValue(params));
        // 注释：执行当前代码行。
        log.setRowCount(rows.size());
        // 注释：执行当前代码行。
        log.setStatus("SUCCESS");
        // 注释：执行当前代码行。
        log.setCreateTime(new Date());
        // 注释：执行当前代码行。
        queryLogDOMapper.insertSelective(log);

        // 注释：返回当前处理结果。
        return rows.stream().map(DomainRecord::of).collect(Collectors.toList());
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
    private long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
