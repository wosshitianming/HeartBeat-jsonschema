package top.kx.heartbeat.infrastructure.pay.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.port.PayNotifyLogRepository;
import top.kx.heartbeat.application.pay.request.PayNotifyLogRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayNotifyLogDO;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayNotifyLogDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.pay.PayNotifyLogDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class PayNotifyLogRepositoryImpl implements PayNotifyLogRepository {

    @Resource
    private PayNotifyLogDOMapper notifyLogMapper;

    @Override
    public DomainRecord recordNotify(PayNotifyLogRequest request) {
        PayNotifyLogRequest safeRequest = request == null ? new PayNotifyLogRequest() : request;
        Date now = new Date();
        PayNotifyLogDO row = new PayNotifyLogDO();
        row.setTenantId(tenantId());
        row.setOrderId(longValue(safeRequest.getOrderId(), 0L));
        row.setOrderNo(stringValue(safeRequest.getOrderNo()));
        row.setProvider(stringValue(safeRequest.getProvider()));
        row.setNotifyId(row.getOrderNo() + "-" + now.getTime());
        row.setSignatureValid(stringValue(safeRequest.getSignatureValid()));
        row.setStatus(stringValue(safeRequest.getStatus()));
        row.setNotifyPayload(stringValue(safeRequest.getPayload()));
        row.setCreateTime(now);
        row.setUpdateTime(now);
        notifyLogMapper.insertSelective(row);
        return record(row);
    }

    @Override
    public List<DomainRecord> listNotifyLogs(String orderNo) {
        PayNotifyLogDOExample example = new PayNotifyLogDOExample();
        PayNotifyLogDOExample.Criteria criteria = example.createCriteria().andTenantIdEqualTo(tenantId());
        if (StringUtils.isNotBlank(orderNo)) {
            criteria.andOrderNoEqualTo(orderNo);
        }
        example.setOrderByClause("create_time DESC, id DESC");
        return notifyLogMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::record)
                .collect(Collectors.toList());
    }

    private DomainRecord record(PayNotifyLogDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", stringValue(row.getId()));
        values.put("tenantId", stringValue(row.getTenantId()));
        values.put("orderId", stringValue(row.getOrderId()));
        values.put("orderNo", row.getOrderNo());
        values.put("provider", row.getProvider());
        values.put("notifyId", row.getNotifyId());
        values.put("signatureValid", row.getSignatureValid());
        values.put("status", row.getStatus());
        values.put("payload", row.getNotifyPayload());
        values.put("createTime", stringValue(row.getCreateTime()));
        values.put("updateTime", stringValue(row.getUpdateTime()));
        return DomainRecord.of(values);
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
