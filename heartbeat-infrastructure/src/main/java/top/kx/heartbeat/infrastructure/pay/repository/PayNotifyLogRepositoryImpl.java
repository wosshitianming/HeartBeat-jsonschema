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
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PayNotifyLogRequest safeRequest = request == null ? new PayNotifyLogRequest() : request;
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 创建数据库记录对象，承载即将写入的业务字段。
        PayNotifyLogDO row = new PayNotifyLogDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setTenantId(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setOrderId(longValue(safeRequest.getOrderId(), 0L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setOrderNo(stringValue(safeRequest.getOrderNo()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setProvider(stringValue(safeRequest.getProvider()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setNotifyId(row.getOrderNo() + "-" + now.getTime());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setSignatureValid(stringValue(safeRequest.getSignatureValid()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setStatus(stringValue(safeRequest.getStatus()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setNotifyPayload(stringValue(safeRequest.getPayload()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setCreateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        notifyLogMapper.insertSelective(row);
        // 返回已经完成封装的业务结果。
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
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        PayNotifyLogDOExample example = new PayNotifyLogDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        PayNotifyLogDOExample.Criteria criteria = example.createCriteria().andTenantIdEqualTo(tenantId());
        // 根据当前业务条件选择对应处理路径。
        if (StringUtils.isNotBlank(orderNo)) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            criteria.andOrderNoEqualTo(orderNo);
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("create_time DESC, id DESC");
        // 返回已经完成封装的业务结果。
        return notifyLogMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::record)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(PayNotifyLogDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", stringValue(row.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", stringValue(row.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("orderId", stringValue(row.getOrderId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("orderNo", row.getOrderNo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("provider", row.getProvider());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("notifyId", row.getNotifyId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("signatureValid", row.getSignatureValid());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("payload", row.getNotifyPayload());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", stringValue(row.getCreateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("updateTime", stringValue(row.getUpdateTime()));
        // 返回已经完成封装的业务结果。
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
        // 根据当前业务条件选择对应处理路径。
        if (raw instanceof Number) {
            // 返回已经完成封装的业务结果。
            return ((Number) raw).longValue();
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
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
