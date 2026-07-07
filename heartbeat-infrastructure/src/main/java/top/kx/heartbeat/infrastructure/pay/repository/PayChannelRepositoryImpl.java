package top.kx.heartbeat.infrastructure.pay.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.port.PayChannelRepository;
import top.kx.heartbeat.application.pay.request.PayChannelRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayChannelDO;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayChannelDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.pay.PayChannelDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class PayChannelRepositoryImpl implements PayChannelRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private PayChannelDOMapper channelMapper;

    @Override
    public List<DomainRecord> listChannels() {
        PayChannelDOExample example = new PayChannelDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("sort_no ASC, id DESC");
        return channelMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::record)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord getChannel(String id) {
        return record(requireChannel(id));
    }

    @Override
    public DomainRecord createChannel(PayChannelRequest request) {
        PayChannelDO row = channelRow(request);
        Date now = new Date();
        row.setTenantId(tenantId());
        row.setCreateTime(now);
        row.setUpdateTime(now);
        channelMapper.insertSelective(row);
        return record(row);
    }

    @Override
    public DomainRecord updateChannel(String id, PayChannelRequest request) {
        PayChannelDO row = requireChannel(id);
        merge(row, request);
        row.setUpdateTime(new Date());
        channelMapper.updateByPrimaryKeySelective(row);
        return record(requireChannel(String.valueOf(row.getId())));
    }

    private PayChannelDO channelRow(PayChannelRequest request) {
        PayChannelDO row = new PayChannelDO();
        merge(row, request);
        return row;
    }

    private void merge(PayChannelDO row, PayChannelRequest request) {
        PayChannelRequest safeRequest = request == null ? new PayChannelRequest() : request;
        row.setName(value(safeRequest.getName(), value(row.getName(), "支付渠道")));
        row.setProvider(value(safeRequest.getProvider(), value(row.getProvider(), "MOCK")));
        row.setAppId(value(safeRequest.getAppId(), value(row.getAppId(), "")));
        row.setAppSecret(value(safeRequest.getAppSecret(), value(row.getAppSecret(), "")));
        row.setStatus(value(safeRequest.getStatus(), value(row.getStatus(), "ACTIVE")));
        row.setSortNo(safeRequest.getSortNo() == null ? intValue(row.getSortNo(), 0) : safeRequest.getSortNo());
        Object config = safeRequest.getConfig() == null ? row.getConfigJson() : safeRequest.getConfig();
        row.setConfigJson(jsonValue(config));
    }

    private PayChannelDO requireChannel(String id) {
        PayChannelDO row = findChannelById(longValue(id, -1L));
        if (row == null) {
            throw new IllegalArgumentException("Pay channel does not exist: " + id);
        }
        return row;
    }

    private PayChannelDO findChannelById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        PayChannelDO row = channelMapper.selectByPrimaryKey(id);
        return row != null && tenantId().equals(row.getTenantId()) ? row : null;
    }

    private DomainRecord record(PayChannelDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", stringValue(row.getId()));
        values.put("tenantId", stringValue(row.getTenantId()));
        values.put("name", row.getName());
        values.put("provider", row.getProvider());
        values.put("appId", row.getAppId());
        values.put("appSecret", row.getAppSecret());
        values.put("status", row.getStatus());
        values.put("sortNo", row.getSortNo());
        values.put("config", readJson(row.getConfigJson()));
        values.put("createTime", stringValue(row.getCreateTime()));
        values.put("updateTime", stringValue(row.getUpdateTime()));
        return DomainRecord.of(values);
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
            if (value == null) {
                return "{}";
            }
            if (value instanceof String) {
                String text = ((String) value).trim();
                return StringUtils.isBlank(text) ? "{}" : text;
            }
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    private String value(Object raw, String defaultValue) {
        String text = stringValue(raw);
        return StringUtils.isBlank(text) ? defaultValue : text;
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
