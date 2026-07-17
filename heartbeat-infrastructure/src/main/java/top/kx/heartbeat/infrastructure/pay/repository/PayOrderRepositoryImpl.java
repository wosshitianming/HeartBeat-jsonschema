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
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PayOrderRepositoryImpl implements PayOrderRepository {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private PayOrderDOMapper orderMapper;

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createOrder(PayOrderRequest request) {
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PayOrderRequest safeRequest = request == null ? new PayOrderRequest() : request;
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 创建数据库记录对象，承载即将写入的业务字段。
        PayOrderDO row = new PayOrderDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setTenantId(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setOrderNo(value(safeRequest.getOrderNo(), "PAY" + System.currentTimeMillis()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setChannelId(longValue(safeRequest.getChannelId(), 0L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setSubject(value(safeRequest.getSubject(), "支付订单"));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setAmount(safeRequest.getAmount() == null ? BigDecimal.ZERO : safeRequest.getAmount());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setCurrency(value(safeRequest.getCurrency(), "CNY"));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setStatus(value(safeRequest.getStatus(), PayOrderStatus.PAYING.getCode()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setClientIp(value(safeRequest.getClientIp(), ""));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setExtraJson(jsonValue(safeRequest.getExtra()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setCreateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        orderMapper.insertSelective(row);
        // 返回已经完成封装的业务结果。
        return record(row);
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord getOrder(String id) {
        return record(requireOrder(id));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listOrders() {
        PayOrderDOExample example = new PayOrderDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return orderMapper.selectByExampleWithBLOBs(example).stream().map(this::record).collect(Collectors.toList());
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，通过 Mapper 完成公众号管理数据访问。
     *
     * @param orderNo 业务处理所需参数。
     * @param status 目标业务状态。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord updateOrderStatus(String orderNo, String status) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        PayOrderDO order = requireOrder(orderNo);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        PayOrderStatus currentStatus = PayOrderStatus.fromCode(order.getStatus());
        // 计算当前分支的中间结果，供后续判断或组装使用。
        PayOrderStatus targetStatus = PayOrderStatus.fromCode(status);
        // 根据当前业务条件选择对应处理路径。
        if (currentStatus == targetStatus) {
            // 返回已经完成封装的业务结果。
            return record(order);
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        boolean directPaid = currentStatus == PayOrderStatus.CREATED && targetStatus == PayOrderStatus.PAID;
        // 根据当前业务条件选择对应处理路径。
        if (!currentStatus.canTransitTo(targetStatus) && !directPaid) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("Invalid pay order status transition: "
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    + currentStatus.getCode() + " -> " + targetStatus.getCode());
        }
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        order.setStatus(targetStatus.getCode());
        // 根据当前业务条件选择对应处理路径。
        if (targetStatus == PayOrderStatus.PAID) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            order.setPaidAt(now);
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        order.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        orderMapper.updateByPrimaryKeySelective(order);
        // 返回已经完成封装的业务结果。
        return record(order);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private PayOrderDO requireOrder(String id) {
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        PayOrderDO row = orderMapper.selectByPrimaryKey(longValue(id, -1L));
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (row != null && tenantId().equals(row.getTenantId())) {
            // 返回已经完成封装的业务结果。
            return row;
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        PayOrderDOExample example = new PayOrderDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andOrderNoEqualTo(id);
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        List<PayOrderDO> rows = orderMapper.selectByExampleWithBLOBs(example);
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (rows.isEmpty()) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Pay order does not exist: " + id);
        }
        // 返回已经完成封装的业务结果。
        return rows.get(0);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(PayOrderDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", stringValue(row.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", stringValue(row.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("orderNo", row.getOrderNo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("channelId", stringValue(row.getChannelId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("subject", row.getSubject());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("amount", row.getAmount());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("currency", row.getCurrency());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("clientIp", row.getClientIp());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("extra", readJson(row.getExtraJson()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", stringValue(row.getCreateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("updateTime", stringValue(row.getUpdateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("paidAt", stringValue(row.getPaidAt()));
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param json 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private JsonNode readJson(String json) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
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
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (value == null) {
                // 返回已经完成封装的业务结果。
                return "{}";
            }
            // 根据当前业务条件选择对应处理路径。
            if (value instanceof String) {
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                String text = ((String) value).trim();
                // 返回已经完成封装的业务结果。
                return StringUtils.isBlank(text) ? "{}" : text;
            }
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
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
