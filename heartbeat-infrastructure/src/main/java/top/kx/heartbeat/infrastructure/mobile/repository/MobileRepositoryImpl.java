package top.kx.heartbeat.infrastructure.mobile.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.port.MobileRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileApiRouteDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileApiRouteDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppVersionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppVersionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobilePageDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobilePageDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobileApiRouteDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobileAppDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobileAppVersionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobilePageDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class MobileRepositoryImpl implements MobileRepository {

    @Autowired
    private MobileAppDOMapper appDOMapper;

    @Autowired
    private MobilePageDOMapper pageDOMapper;

    @Autowired
    private MobileApiRouteDOMapper apiRouteDOMapper;

    @Autowired
    private MobileAppVersionDOMapper versionDOMapper;

    @Override
    public List<DomainRecord> listApps() {
        MobileAppDOExample example = new MobileAppDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return appDOMapper.selectByExample(example)
                .stream()
                .map(this::toAppRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord saveApp(Map<String, Object> command) {
        Date now = new Date();
        MobileAppDO record = new MobileAppDO();
        record.setTenantId(tenantId());
        record.setName(value(command, "name", "移动应用"));
        record.setAppKey(value(command, "appKey", value(command, "app_key", "")));
        record.setEntryUrl(value(command, "entryUrl", value(command, "entry_url", "")));
        record.setStatus(value(command, "status", "DRAFT"));
        record.setConfigJson(jsonValue(command.get("config")));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        appDOMapper.insertSelective(record);

        if ("PUBLISHED".equals(record.getStatus())) {
            MobileAppVersionDO version = new MobileAppVersionDO();
            version.setTenantId(tenantId());
            version.setAppId(record.getId());
            version.setVersionNo(1);
            version.setSchemaJson(record.getConfigJson());
            version.setStatus("PUBLISHED");
            version.setPublishedAt(now);
            version.setCreateTime(now);
            versionDOMapper.insertSelective(version);
        }

        return toAppRecord(record);
    }

    @Override
    public List<DomainRecord> listPages(String appId) {
        MobilePageDOExample example = new MobilePageDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(longValue(appId, 0L));
        example.setOrderByClause("sort_no, id");
        return pageDOMapper.selectByExample(example)
                .stream()
                .map(this::toPageRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord savePage(Map<String, Object> command) {
        Date now = new Date();
        MobilePageDO record = new MobilePageDO();
        record.setTenantId(tenantId());
        record.setAppId(longValue(value(command, "appId", value(command, "app_id", "0")), 0L));
        record.setName(value(command, "name", "页面"));
        record.setPageKey(value(command, "pageKey", value(command, "page_key", "")));
        record.setRoutePath(value(command, "routePath", value(command, "route_path", "")));
        record.setSchemaJson(jsonValue(command.get("schema")));
        record.setSortNo(intValue(command.get("sortNo"), 0));
        record.setStatus(value(command, "status", "DRAFT"));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        pageDOMapper.insertSelective(record);
        return toPageRecord(record);
    }

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
    public DomainRecord saveApiRoute(Map<String, Object> command) {
        Date now = new Date();
        MobileApiRouteDO record = new MobileApiRouteDO();
        record.setTenantId(tenantId());
        record.setAppId(longValue(value(command, "appId", value(command, "app_id", "0")), 0L));
        record.setName(value(command, "name", "接口"));
        record.setRouteKey(value(command, "routeKey", value(command, "route_key", "")));
        record.setMethod(value(command, "method", "GET"));
        record.setPath(value(command, "path", ""));
        record.setTargetUrl(value(command, "targetUrl", value(command, "target_url", "")));
        record.setSortNo(intValue(command.get("sortNo"), 0));
        record.setStatus(value(command, "status", "ACTIVE"));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        apiRouteDOMapper.insertSelective(record);
        return toApiRouteRecord(record);
    }

    private DomainRecord toAppRecord(MobileAppDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entity.getId()));
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        row.put("name", entity.getName());
        row.put("appKey", entity.getAppKey());
        row.put("entryUrl", entity.getEntryUrl());
        row.put("status", entity.getStatus());
        row.put("config", readJson(entity.getConfigJson()));
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    private DomainRecord toPageRecord(MobilePageDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entity.getId()));
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        row.put("appId", String.valueOf(entity.getAppId()));
        row.put("name", entity.getName());
        row.put("pageKey", entity.getPageKey());
        row.put("routePath", entity.getRoutePath());
        row.put("schema", readJson(entity.getSchemaJson()));
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        return DomainRecord.of(row);
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

    private com.fasterxml.jackson.databind.JsonNode readJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 解析失败", ex);
        }
    }

    private String jsonValue(Object value) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 序列化失败", ex);
        }
    }

    private String value(Map<String, Object> command, String key, String defaultValue) {
        Object value = command.get(key);
        return StringUtils.isBlank(value == null ? null : String.valueOf(value)) ? defaultValue : String.valueOf(value).trim();
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
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
