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

@Repository
public class MobileApiRouteRepositoryImpl implements MobileApiRouteRepository {

    @Resource
    private MobileApiRouteDOMapper apiRouteDOMapper;

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

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
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
