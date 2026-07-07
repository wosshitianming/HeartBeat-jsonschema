package top.kx.heartbeat.infrastructure.mp.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.port.MpMenuRepository;
import top.kx.heartbeat.application.mp.request.MpMenuRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMenuDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMenuDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpMenuDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class MpMenuRepositoryImpl implements MpMenuRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private MpMenuDOMapper menuDOMapper;

    @Override
    public List<DomainRecord> listMenus(String accountId) {
        MpMenuDOExample example = new MpMenuDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAccountIdEqualTo(longValue(accountId, 0L));
        example.setOrderByClause("sort_no ASC, id ASC");
        return menuDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toMenuRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord saveMenu(MpMenuRequest request) {
        Date now = new Date();
        MpMenuDO record = findMenuById(longValue(request.getId(), -1L));
        if (record == null) {
            record = new MpMenuDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            applyMenu(record, request);
            record.setUpdateTime(now);
            menuDOMapper.insertSelective(record);
            return toMenuRecord(record);
        }
        applyMenu(record, request);
        record.setUpdateTime(now);
        menuDOMapper.updateByPrimaryKeySelective(record);
        return toMenuRecord(record);
    }

    private void applyMenu(MpMenuDO record, MpMenuRequest request) {
        record.setAccountId(longValue(defaultText(request.getAccountId(), stringValue(record.getAccountId())), 0L));
        record.setParentId(longValue(defaultText(request.getParentId(), stringValue(record.getParentId())), 0L));
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "Menu")));
        record.setMenuType(defaultText(request.getMenuType(), defaultText(record.getMenuType(), "view")));
        record.setUrl(defaultText(request.getUrl(), defaultText(record.getUrl(), "")));
        record.setSortNo(request.getSortNo() == null ? intValue(record.getSortNo(), 0) : request.getSortNo());
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
        record.setPayload(request.getPayload() == null ? defaultJson(record.getPayload()) : jsonValue(request.getPayload()));
    }

    private MpMenuDO findMenuById(long id) {
        if (id <= 0) {
            return null;
        }
        MpMenuDO record = menuDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    private DomainRecord toMenuRecord(MpMenuDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("accountId", stringValue(entity.getAccountId()));
        row.put("parentId", stringValue(entity.getParentId()));
        row.put("name", entity.getName());
        row.put("menuType", entity.getMenuType());
        row.put("url", entity.getUrl());
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        row.put("payload", readJson(entity.getPayload()));
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
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
            if (value instanceof String) {
                String text = ((String) value).trim();
                return StringUtils.isBlank(text) ? "{}" : text;
            }
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    private String defaultJson(String value) {
        return StringUtils.isBlank(value) ? "{}" : value;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    private int intValue(Object raw, int defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return raw == null ? defaultValue : Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private long longValue(Object raw, long defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        try {
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
