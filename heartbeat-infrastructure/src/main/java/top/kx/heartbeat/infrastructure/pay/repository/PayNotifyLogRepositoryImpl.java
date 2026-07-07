// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PayNotifyLogRepositoryImpl implements PayNotifyLogRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PayNotifyLogDOMapper notifyLogMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord recordNotify(PayNotifyLogRequest request) {
        // 注释：设置或计算当前变量值。
        PayNotifyLogRequest safeRequest = request == null ? new PayNotifyLogRequest() : request;
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        PayNotifyLogDO row = new PayNotifyLogDO();
        // 注释：执行当前代码行。
        row.setTenantId(tenantId());
        // 注释：执行当前代码行。
        row.setOrderId(longValue(safeRequest.getOrderId(), 0L));
        // 注释：执行当前代码行。
        row.setOrderNo(stringValue(safeRequest.getOrderNo()));
        // 注释：执行当前代码行。
        row.setProvider(stringValue(safeRequest.getProvider()));
        // 注释：执行当前代码行。
        row.setNotifyId(row.getOrderNo() + "-" + now.getTime());
        // 注释：执行当前代码行。
        row.setSignatureValid(stringValue(safeRequest.getSignatureValid()));
        // 注释：执行当前代码行。
        row.setStatus(stringValue(safeRequest.getStatus()));
        // 注释：执行当前代码行。
        row.setNotifyPayload(stringValue(safeRequest.getPayload()));
        // 注释：执行当前代码行。
        row.setCreateTime(now);
        // 注释：执行当前代码行。
        row.setUpdateTime(now);
        // 注释：执行当前代码行。
        notifyLogMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return record(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listNotifyLogs(String orderNo) {
        // 注释：设置或计算当前变量值。
        PayNotifyLogDOExample example = new PayNotifyLogDOExample();
        // 注释：设置或计算当前变量值。
        PayNotifyLogDOExample.Criteria criteria = example.createCriteria().andTenantIdEqualTo(tenantId());
        // 注释：判断当前业务条件。
        if (StringUtils.isNotBlank(orderNo)) {
            // 注释：执行当前代码行。
            criteria.andOrderNoEqualTo(orderNo);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return notifyLogMapper.selectByExampleWithBLOBs(example)
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
    private DomainRecord record(PayNotifyLogDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", stringValue(row.getId()));
        // 注释：执行当前代码行。
        values.put("tenantId", stringValue(row.getTenantId()));
        // 注释：执行当前代码行。
        values.put("orderId", stringValue(row.getOrderId()));
        // 注释：执行当前代码行。
        values.put("orderNo", row.getOrderNo());
        // 注释：执行当前代码行。
        values.put("provider", row.getProvider());
        // 注释：执行当前代码行。
        values.put("notifyId", row.getNotifyId());
        // 注释：执行当前代码行。
        values.put("signatureValid", row.getSignatureValid());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("payload", row.getNotifyPayload());
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
