package top.kx.heartbeat.infrastructure.mobile.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.port.MobilePageRepository;
import top.kx.heartbeat.application.mobile.request.MobilePageRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobilePageDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobilePageDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobilePageDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class MobilePageRepositoryImpl implements MobilePageRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private MobilePageDOMapper pageDOMapper;

    @Override
    public List<DomainRecord> listPages(String appId) {
        MobilePageDOExample example = new MobilePageDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(longValue(appId, 0L));
        example.setOrderByClause("sort_no, id");
        return pageDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toPageRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord savePage(MobilePageRequest request) {
        Date now = new Date();
        MobilePageDO record = new MobilePageDO();
        record.setTenantId(tenantId());
        record.setAppId(longValue(request.getAppId(), 0L));
        record.setName(defaultText(request.getName(), "Page"));
        record.setPageKey(defaultText(request.getPageKey(), ""));
        record.setRoutePath(defaultText(request.getRoutePath(), ""));
        record.setSchemaJson(jsonValue(request.getSchema()));
        record.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        record.setStatus(defaultText(request.getStatus(), "DRAFT"));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        pageDOMapper.insertSelective(record);
        return toPageRecord(record);
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
