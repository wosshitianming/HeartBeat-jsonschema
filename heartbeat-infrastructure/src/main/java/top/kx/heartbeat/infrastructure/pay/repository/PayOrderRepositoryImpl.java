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

@Repository
public class PayOrderRepositoryImpl implements PayOrderRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private PayOrderDOMapper orderMapper;

    @Override
    public DomainRecord createOrder(PayOrderRequest request) {
        PayOrderRequest safeRequest = request == null ? new PayOrderRequest() : request;
        Date now = new Date();
        PayOrderDO row = new PayOrderDO();
        row.setTenantId(tenantId());
        row.setOrderNo(value(safeRequest.getOrderNo(), "PAY" + System.currentTimeMillis()));
        row.setChannelId(longValue(safeRequest.getChannelId(), 0L));
        row.setSubject(value(safeRequest.getSubject(), "支付订单"));
        row.setAmount(safeRequest.getAmount() == null ? BigDecimal.ZERO : safeRequest.getAmount());
        row.setCurrency(value(safeRequest.getCurrency(), "CNY"));
        row.setStatus(value(safeRequest.getStatus(), PayOrderStatus.PAYING.getCode()));
        row.setClientIp(value(safeRequest.getClientIp(), ""));
        row.setExtraJson(jsonValue(safeRequest.getExtra()));
        row.setCreateTime(now);
        row.setUpdateTime(now);
        orderMapper.insertSelective(row);
        return record(row);
    }

    @Override
    public DomainRecord getOrder(String id) {
        return record(requireOrder(id));
    }

    @Override
    public List<DomainRecord> listOrders() {
        PayOrderDOExample example = new PayOrderDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return orderMapper.selectByExampleWithBLOBs(example).stream().map(this::record).collect(Collectors.toList());
    }

    @Override
    public DomainRecord updateOrderStatus(String orderNo, String status) {
        PayOrderDO order = requireOrder(orderNo);
        PayOrderStatus currentStatus = PayOrderStatus.fromCode(order.getStatus());
        PayOrderStatus targetStatus = PayOrderStatus.fromCode(status);
        if (currentStatus == targetStatus) {
            return record(order);
        }
        boolean directPaid = currentStatus == PayOrderStatus.CREATED && targetStatus == PayOrderStatus.PAID;
        if (!currentStatus.canTransitTo(targetStatus) && !directPaid) {
            throw new IllegalStateException("Invalid pay order status transition: "
                    + currentStatus.getCode() + " -> " + targetStatus.getCode());
        }
        Date now = new Date();
        order.setStatus(targetStatus.getCode());
        if (targetStatus == PayOrderStatus.PAID) {
            order.setPaidAt(now);
        }
        order.setUpdateTime(now);
        orderMapper.updateByPrimaryKeySelective(order);
        return record(order);
    }

    private PayOrderDO requireOrder(String id) {
        PayOrderDO row = orderMapper.selectByPrimaryKey(longValue(id, -1L));
        if (row != null && tenantId().equals(row.getTenantId())) {
            return row;
        }
        PayOrderDOExample example = new PayOrderDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andOrderNoEqualTo(id);
        List<PayOrderDO> rows = orderMapper.selectByExampleWithBLOBs(example);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Pay order does not exist: " + id);
        }
        return rows.get(0);
    }

    private DomainRecord record(PayOrderDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", stringValue(row.getId()));
        values.put("tenantId", stringValue(row.getTenantId()));
        values.put("orderNo", row.getOrderNo());
        values.put("channelId", stringValue(row.getChannelId()));
        values.put("subject", row.getSubject());
        values.put("amount", row.getAmount());
        values.put("currency", row.getCurrency());
        values.put("status", row.getStatus());
        values.put("clientIp", row.getClientIp());
        values.put("extra", readJson(row.getExtraJson()));
        values.put("createTime", stringValue(row.getCreateTime()));
        values.put("updateTime", stringValue(row.getUpdateTime()));
        values.put("paidAt", stringValue(row.getPaidAt()));
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
