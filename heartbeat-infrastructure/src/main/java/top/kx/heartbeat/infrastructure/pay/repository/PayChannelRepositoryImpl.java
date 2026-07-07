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

/**
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PayChannelRepositoryImpl implements PayChannelRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private PayChannelDOMapper channelMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
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

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord getChannel(String id) {
        return record(requireChannel(id));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 更新业务记录，只处理调用方传入的可变字段，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord updateChannel(String id, PayChannelRequest request) {
        PayChannelDO row = requireChannel(id);
        merge(row, request);
        row.setUpdateTime(new Date());
        channelMapper.updateByPrimaryKeySelective(row);
        return record(requireChannel(String.valueOf(row.getId())));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    private PayChannelDO channelRow(PayChannelRequest request) {
        PayChannelDO row = new PayChannelDO();
        merge(row, request);
        return row;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @param request 公众号管理请求参数。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private PayChannelDO requireChannel(String id) {
        PayChannelDO row = findChannelById(longValue(id, -1L));
        if (row == null) {
            throw new IllegalArgumentException("Pay channel does not exist: " + id);
        }
        return row;
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private PayChannelDO findChannelById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        PayChannelDO row = channelMapper.selectByPrimaryKey(id);
        return row != null && tenantId().equals(row.getTenantId()) ? row : null;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param json 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON parse failed", ex);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param raw 业务处理所需参数。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String value(Object raw, String defaultValue) {
        String text = stringValue(raw);
        return StringUtils.isBlank(text) ? defaultValue : text;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param raw 业务处理所需参数。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param raw 业务处理所需参数。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
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

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
