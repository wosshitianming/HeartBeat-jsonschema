package top.kx.heartbeat.infrastructure.mobile.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.port.MobileAppRepository;
import top.kx.heartbeat.application.mobile.request.MobileAppRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppVersionDO;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobileAppDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobileAppVersionDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class MobileAppRepositoryImpl implements MobileAppRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private MobileAppDOMapper appDOMapper;

    @Resource
    private MobileAppVersionDOMapper versionDOMapper;

    @Override
    public List<DomainRecord> listApps() {
        MobileAppDOExample example = new MobileAppDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return appDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toAppRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord saveApp(MobileAppRequest request) {
        Date now = new Date();
        MobileAppDO record = new MobileAppDO();
        record.setTenantId(tenantId());
        record.setName(defaultText(request.getName(), "Mobile App"));
        record.setAppKey(defaultText(request.getAppKey(), ""));
        record.setEntryUrl(defaultText(request.getEntryUrl(), ""));
        record.setStatus(defaultText(request.getStatus(), "DRAFT"));
        record.setConfigJson(jsonValue(request.getConfig()));
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

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON parse failed", ex);
        }
    }

    private String jsonValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
