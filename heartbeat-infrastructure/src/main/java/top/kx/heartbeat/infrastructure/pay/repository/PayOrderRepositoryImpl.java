// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.pay.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.port.PayOrderRepository;
import top.kx.heartbeat.application.pay.request.PayOrderRequest;
import top.kx.heartbeat.domain.pay.PayOrderStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayOrderDO;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayOrderDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.pay.PayOrderDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.math.BigDecimal;
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
public class PayOrderRepositoryImpl implements PayOrderRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PayOrderDOMapper orderMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createOrder(PayOrderRequest request) {
        // 注释：设置或计算当前变量值。
        PayOrderRequest safeRequest = request == null ? new PayOrderRequest() : request;
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        PayOrderDO row = new PayOrderDO();
        // 注释：执行当前代码行。
        row.setTenantId(tenantId());
        // 注释：执行当前代码行。
        row.setOrderNo(value(safeRequest.getOrderNo(), "PAY" + System.currentTimeMillis()));
        // 注释：执行当前代码行。
        row.setChannelId(longValue(safeRequest.getChannelId(), 0L));
        // 注释：执行当前代码行。
        row.setSubject(value(safeRequest.getSubject(), "支付订单"));
        // 注释：设置或计算当前变量值。
        row.setAmount(safeRequest.getAmount() == null ? BigDecimal.ZERO : safeRequest.getAmount());
        // 注释：执行当前代码行。
        row.setCurrency(value(safeRequest.getCurrency(), "CNY"));
        // 注释：执行当前代码行。
        row.setStatus(value(safeRequest.getStatus(), PayOrderStatus.PAYING.getCode()));
        // 注释：执行当前代码行。
        row.setClientIp(value(safeRequest.getClientIp(), ""));
        // 注释：执行当前代码行。
        row.setExtraJson(jsonValue(safeRequest.getExtra()));
        // 注释：执行当前代码行。
        row.setCreateTime(now);
        // 注释：执行当前代码行。
        row.setUpdateTime(now);
        // 注释：执行当前代码行。
        orderMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return record(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord getOrder(String id) {
        // 注释：返回当前处理结果。
        return record(requireOrder(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listOrders() {
        // 注释：设置或计算当前变量值。
        PayOrderDOExample example = new PayOrderDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return orderMapper.selectByExampleWithBLOBs(example).stream().map(this::record).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord updateOrderStatus(String orderNo, String status) {
        // 注释：设置或计算当前变量值。
        PayOrderDO order = requireOrder(orderNo);
        // 注释：设置或计算当前变量值。
        PayOrderStatus currentStatus = PayOrderStatus.fromCode(order.getStatus());
        // 注释：设置或计算当前变量值。
        PayOrderStatus targetStatus = PayOrderStatus.fromCode(status);
        // 注释：判断当前业务条件。
        if (currentStatus == targetStatus) {
            // 注释：返回当前处理结果。
            return record(order);
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        boolean directPaid = currentStatus == PayOrderStatus.CREATED && targetStatus == PayOrderStatus.PAID;
        // 注释：判断当前业务条件。
        if (!currentStatus.canTransitTo(targetStatus) && !directPaid) {
            // 注释：抛出当前业务异常。
            throw new IllegalStateException("Invalid pay order status transition: "
                    // 注释：执行当前代码行。
                    + currentStatus.getCode() + " -> " + targetStatus.getCode());
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：执行当前代码行。
        order.setStatus(targetStatus.getCode());
        // 注释：判断当前业务条件。
        if (targetStatus == PayOrderStatus.PAID) {
            // 注释：执行当前代码行。
            order.setPaidAt(now);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        order.setUpdateTime(now);
        // 注释：执行当前代码行。
        orderMapper.updateByPrimaryKeySelective(order);
        // 注释：返回当前处理结果。
        return record(order);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private PayOrderDO requireOrder(String id) {
        // 注释：设置或计算当前变量值。
        PayOrderDO row = orderMapper.selectByPrimaryKey(longValue(id, -1L));
        // 注释：判断当前业务条件。
        if (row != null && tenantId().equals(row.getTenantId())) {
            // 注释：返回当前处理结果。
            return row;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        PayOrderDOExample example = new PayOrderDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andOrderNoEqualTo(id);
        // 注释：设置或计算当前变量值。
        List<PayOrderDO> rows = orderMapper.selectByExampleWithBLOBs(example);
        // 注释：判断当前业务条件。
        if (rows.isEmpty()) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Pay order does not exist: " + id);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return rows.get(0);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(PayOrderDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", stringValue(row.getId()));
        // 注释：执行当前代码行。
        values.put("tenantId", stringValue(row.getTenantId()));
        // 注释：执行当前代码行。
        values.put("orderNo", row.getOrderNo());
        // 注释：执行当前代码行。
        values.put("channelId", stringValue(row.getChannelId()));
        // 注释：执行当前代码行。
        values.put("subject", row.getSubject());
        // 注释：执行当前代码行。
        values.put("amount", row.getAmount());
        // 注释：执行当前代码行。
        values.put("currency", row.getCurrency());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("clientIp", row.getClientIp());
        // 注释：执行当前代码行。
        values.put("extra", readJson(row.getExtraJson()));
        // 注释：执行当前代码行。
        values.put("createTime", stringValue(row.getCreateTime()));
        // 注释：执行当前代码行。
        values.put("updateTime", stringValue(row.getUpdateTime()));
        // 注释：执行当前代码行。
        values.put("paidAt", stringValue(row.getPaidAt()));
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
