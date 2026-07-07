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
 * 实现移动端配置持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class MobileApiRouteRepositoryImpl implements MobileApiRouteRepository {

    @Resource
    private MobileApiRouteDOMapper apiRouteDOMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成移动端配置数据访问。
     *
     * @param appId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listApiRoutes(String appId) {
        MobileApiRouteDOExample example = new MobileApiRouteDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(longValue(appId, 0L));
        example.setOrderByClause("sort_no, id");
        return apiRouteDOMapper.selectByExample(example)
                .stream()
                .map(this::toApiRouteRecord)
                .collect(Collectors.toList());
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成移动端配置数据访问。
     *
     * @param request 移动端配置请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord saveApiRoute(MobileApiRouteRequest request) {
        Date now = new Date();
        MobileApiRouteDO record = new MobileApiRouteDO();
        record.setTenantId(tenantId());
        record.setAppId(longValue(request.getAppId(), 0L));
        record.setName(defaultText(request.getName(), "API"));
        record.setRouteKey(defaultText(request.getRouteKey(), ""));
        record.setMethod(defaultText(request.getMethod(), "GET"));
        record.setPath(defaultText(request.getPath(), ""));
        record.setTargetUrl(defaultText(request.getTargetUrl(), ""));
        record.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        record.setStatus(defaultText(request.getStatus(), "ACTIVE"));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        apiRouteDOMapper.insertSelective(record);
        return toApiRouteRecord(record);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成移动端配置数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord toApiRouteRecord(MobileApiRouteDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entity.getId()));
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        row.put("appId", String.valueOf(entity.getAppId()));
        row.put("name", entity.getName());
        row.put("routeKey", entity.getRouteKey());
        row.put("method", entity.getMethod());
        row.put("path", entity.getPath());
        row.put("targetUrl", entity.getTargetUrl());
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成移动端配置数据访问。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成移动端配置数据访问。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private long longValue(String value, long defaultValue) {
        try {
            return StringUtils.isBlank(value) ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成移动端配置数据访问。
     *
     * @return 处理后的业务结果。
     */
    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
