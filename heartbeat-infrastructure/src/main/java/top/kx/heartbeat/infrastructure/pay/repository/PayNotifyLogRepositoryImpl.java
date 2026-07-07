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

/**
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PayNotifyLogRepositoryImpl implements PayNotifyLogRepository {

    @Resource
    private PayNotifyLogDOMapper notifyLogMapper;

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @param orderNo 业务处理所需参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
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
