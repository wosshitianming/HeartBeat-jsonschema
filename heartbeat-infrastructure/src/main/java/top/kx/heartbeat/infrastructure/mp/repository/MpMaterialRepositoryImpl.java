package top.kx.heartbeat.infrastructure.mp.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.port.MpMaterialRepository;
import top.kx.heartbeat.application.mp.request.MpMaterialRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMaterialDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMaterialDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpMaterialDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class MpMaterialRepositoryImpl implements MpMaterialRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private MpMaterialDOMapper materialDOMapper;

    @Override
    public List<DomainRecord> listMaterials(String accountId) {
        MpMaterialDOExample example = new MpMaterialDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAccountIdEqualTo(longValue(accountId, 0L));
        example.setOrderByClause("create_time DESC, id DESC");
        return materialDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toMaterialRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord saveMaterial(MpMaterialRequest request) {
        Date now = new Date();
        MpMaterialDO record = findMaterialById(longValue(request.getId(), -1L));
        if (record == null) {
            record = new MpMaterialDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            applyMaterial(record, request);
            record.setUpdateTime(now);
            materialDOMapper.insertSelective(record);
            return toMaterialRecord(record);
        }
        applyMaterial(record, request);
        record.setUpdateTime(now);
        materialDOMapper.updateByPrimaryKeySelective(record);
        return toMaterialRecord(record);
    }

    private void applyMaterial(MpMaterialDO record, MpMaterialRequest request) {
        record.setAccountId(longValue(defaultText(request.getAccountId(), stringValue(record.getAccountId())), 0L));
        record.setMaterialType(defaultText(request.getMaterialType(), defaultText(record.getMaterialType(), "text")));
        record.setTitle(defaultText(request.getTitle(), defaultText(record.getTitle(), "Material")));
        record.setMediaId(defaultText(request.getMediaId(), defaultText(record.getMediaId(), "")));
        record.setUrl(defaultText(request.getUrl(), defaultText(record.getUrl(), "")));
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
        record.setPayload(request.getPayload() == null ? defaultJson(record.getPayload()) : jsonValue(request.getPayload()));
    }

    private MpMaterialDO findMaterialById(long id) {
        if (id <= 0) {
            return null;
        }
        MpMaterialDO record = materialDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    private DomainRecord toMaterialRecord(MpMaterialDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("accountId", stringValue(entity.getAccountId()));
        row.put("materialType", entity.getMaterialType());
        row.put("title", entity.getTitle());
        row.put("mediaId", entity.getMediaId());
        row.put("url", entity.getUrl());
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
