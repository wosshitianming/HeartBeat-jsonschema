// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.mobile.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.port.MobileApiRouteRepository;
import top.kx.heartbeat.application.mobile.request.MobileApiRouteRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileApiRouteDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileApiRouteDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobileApiRouteDOMapper;
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
public class MobileApiRouteRepositoryImpl implements MobileApiRouteRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MobileApiRouteDOMapper apiRouteDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listApiRoutes(String appId) {
        // 注释：设置或计算当前变量值。
        MobileApiRouteDOExample example = new MobileApiRouteDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(longValue(appId, 0L));
        // 注释：执行当前代码行。
        example.setOrderByClause("sort_no, id");
        // 注释：返回当前处理结果。
        return apiRouteDOMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toApiRouteRecord)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord saveApiRoute(MobileApiRouteRequest request) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        MobileApiRouteDO record = new MobileApiRouteDO();
        // 注释：执行当前代码行。
        record.setTenantId(tenantId());
        // 注释：执行当前代码行。
        record.setAppId(longValue(request.getAppId(), 0L));
        // 注释：执行当前代码行。
        record.setName(defaultText(request.getName(), "API"));
        // 注释：执行当前代码行。
        record.setRouteKey(defaultText(request.getRouteKey(), ""));
        // 注释：执行当前代码行。
        record.setMethod(defaultText(request.getMethod(), "GET"));
        // 注释：执行当前代码行。
        record.setPath(defaultText(request.getPath(), ""));
        // 注释：执行当前代码行。
        record.setTargetUrl(defaultText(request.getTargetUrl(), ""));
        // 注释：设置或计算当前变量值。
        record.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        // 注释：执行当前代码行。
        record.setStatus(defaultText(request.getStatus(), "ACTIVE"));
        // 注释：执行当前代码行。
        record.setCreateTime(now);
        // 注释：执行当前代码行。
        record.setUpdateTime(now);
        // 注释：执行当前代码行。
        apiRouteDOMapper.insertSelective(record);
        // 注释：返回当前处理结果。
        return toApiRouteRecord(record);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord toApiRouteRecord(MobileApiRouteDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", String.valueOf(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("appId", String.valueOf(entity.getAppId()));
        // 注释：执行当前代码行。
        row.put("name", entity.getName());
        // 注释：执行当前代码行。
        row.put("routeKey", entity.getRouteKey());
        // 注释：执行当前代码行。
        row.put("method", entity.getMethod());
        // 注释：执行当前代码行。
        row.put("path", entity.getPath());
        // 注释：执行当前代码行。
        row.put("targetUrl", entity.getTargetUrl());
        // 注释：执行当前代码行。
        row.put("sortNo", entity.getSortNo());
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
    private String defaultText(String value, String defaultValue) {
        // 注释：返回当前处理结果。
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private long longValue(String value, long defaultValue) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return StringUtils.isBlank(value) ? defaultValue : Long.parseLong(value.trim());
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
    private long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
