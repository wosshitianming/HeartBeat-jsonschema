// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PayChannelRepositoryImpl implements PayChannelRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PayChannelDOMapper channelMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listChannels() {
        // 注释：设置或计算当前变量值。
        PayChannelDOExample example = new PayChannelDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 注释：执行当前代码行。
        example.setOrderByClause("sort_no ASC, id DESC");
        // 注释：返回当前处理结果。
        return channelMapper.selectByExampleWithBLOBs(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::record)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord getChannel(String id) {
        // 注释：返回当前处理结果。
        return record(requireChannel(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createChannel(PayChannelRequest request) {
        // 注释：设置或计算当前变量值。
        PayChannelDO row = channelRow(request);
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：执行当前代码行。
        row.setTenantId(tenantId());
        // 注释：执行当前代码行。
        row.setCreateTime(now);
        // 注释：执行当前代码行。
        row.setUpdateTime(now);
        // 注释：执行当前代码行。
        channelMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return record(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord updateChannel(String id, PayChannelRequest request) {
        // 注释：设置或计算当前变量值。
        PayChannelDO row = requireChannel(id);
        // 注释：执行当前代码行。
        merge(row, request);
        // 注释：执行当前代码行。
        row.setUpdateTime(new Date());
        // 注释：执行当前代码行。
        channelMapper.updateByPrimaryKeySelective(row);
        // 注释：返回当前处理结果。
        return record(requireChannel(String.valueOf(row.getId())));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private PayChannelDO channelRow(PayChannelRequest request) {
        // 注释：设置或计算当前变量值。
        PayChannelDO row = new PayChannelDO();
        // 注释：执行当前代码行。
        merge(row, request);
        // 注释：返回当前处理结果。
        return row;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void merge(PayChannelDO row, PayChannelRequest request) {
        // 注释：设置或计算当前变量值。
        PayChannelRequest safeRequest = request == null ? new PayChannelRequest() : request;
        // 注释：执行当前代码行。
        row.setName(value(safeRequest.getName(), value(row.getName(), "支付渠道")));
        // 注释：执行当前代码行。
        row.setProvider(value(safeRequest.getProvider(), value(row.getProvider(), "MOCK")));
        // 注释：执行当前代码行。
        row.setAppId(value(safeRequest.getAppId(), value(row.getAppId(), "")));
        // 注释：执行当前代码行。
        row.setAppSecret(value(safeRequest.getAppSecret(), value(row.getAppSecret(), "")));
        // 注释：执行当前代码行。
        row.setStatus(value(safeRequest.getStatus(), value(row.getStatus(), "ACTIVE")));
        // 注释：设置或计算当前变量值。
        row.setSortNo(safeRequest.getSortNo() == null ? intValue(row.getSortNo(), 0) : safeRequest.getSortNo());
        // 注释：设置或计算当前变量值。
        Object config = safeRequest.getConfig() == null ? row.getConfigJson() : safeRequest.getConfig();
        // 注释：执行当前代码行。
        row.setConfigJson(jsonValue(config));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private PayChannelDO requireChannel(String id) {
        // 注释：设置或计算当前变量值。
        PayChannelDO row = findChannelById(longValue(id, -1L));
        // 注释：判断当前业务条件。
        if (row == null) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Pay channel does not exist: " + id);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return row;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private PayChannelDO findChannelById(Long id) {
        // 注释：判断当前业务条件。
        if (id == null || id <= 0) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        PayChannelDO row = channelMapper.selectByPrimaryKey(id);
        // 注释：返回当前处理结果。
        return row != null && tenantId().equals(row.getTenantId()) ? row : null;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(PayChannelDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", stringValue(row.getId()));
        // 注释：执行当前代码行。
        values.put("tenantId", stringValue(row.getTenantId()));
        // 注释：执行当前代码行。
        values.put("name", row.getName());
        // 注释：执行当前代码行。
        values.put("provider", row.getProvider());
        // 注释：执行当前代码行。
        values.put("appId", row.getAppId());
        // 注释：执行当前代码行。
        values.put("appSecret", row.getAppSecret());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("sortNo", row.getSortNo());
        // 注释：执行当前代码行。
        values.put("config", readJson(row.getConfigJson()));
        // 注释：执行当前代码行。
        values.put("createTime", stringValue(row.getCreateTime()));
        // 注释：执行当前代码行。
        values.put("updateTime", stringValue(row.getUpdateTime()));
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private JsonNode readJson(String json) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
            // 注释：捕获并处理当前异常。
        } catch (Exception ex) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("JSON parse failed", ex);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String jsonValue(Object value) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：判断当前业务条件。
            if (value == null) {
                // 注释：返回当前处理结果。
                return "{}";
                // 注释：结束当前代码块。
            }
            // 注释：判断当前业务条件。
            if (value instanceof String) {
                // 注释：设置或计算当前变量值。
                String text = ((String) value).trim();
                // 注释：返回当前处理结果。
                return StringUtils.isBlank(text) ? "{}" : text;
                // 注释：结束当前代码块。
            }
            // 注释：返回当前处理结果。
            return objectMapper.writeValueAsString(value);
            // 注释：捕获并处理当前异常。
        } catch (Exception ex) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("JSON serialize failed", ex);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String value(Object raw, String defaultValue) {
        // 注释：设置或计算当前变量值。
        String text = stringValue(raw);
        // 注释：返回当前处理结果。
        return StringUtils.isBlank(text) ? defaultValue : text;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private int intValue(Object raw, int defaultValue) {
        // 注释：判断当前业务条件。
        if (raw instanceof Number) {
            // 注释：返回当前处理结果。
            return ((Number) raw).intValue();
            // 注释：结束当前代码块。
        }
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return raw == null ? defaultValue : Integer.parseInt(String.valueOf(raw).trim());
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
    private long longValue(Object raw, long defaultValue) {
        // 注释：判断当前业务条件。
        if (raw instanceof Number) {
            // 注释：返回当前处理结果。
            return ((Number) raw).longValue();
            // 注释：结束当前代码块。
        }
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
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
    private String stringValue(Object value) {
        // 注释：返回当前处理结果。
        return value == null ? "" : String.valueOf(value).trim();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
