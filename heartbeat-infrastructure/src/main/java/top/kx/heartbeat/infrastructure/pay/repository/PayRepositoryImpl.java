package top.kx.heartbeat.infrastructure.pay.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.port.PayRepository;
import top.kx.heartbeat.domain.pay.PayNotifyResult;
import top.kx.heartbeat.domain.pay.PayOrderStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayChannelDO;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayChannelDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayNotifyLogDO;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayNotifyLogDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayOrderDO;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayOrderDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.pay.PayChannelDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.pay.PayNotifyLogDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.pay.PayOrderDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付应用仓储实现。
 *
 * <p>承接应用层支付仓储端口，使用 MyBatis Generator 的 Example/Criteria 完成支付渠道、
 * 支付订单和支付通知日志的持久化读写。</p>
 */
@Repository
public class PayRepositoryImpl implements PayRepository {

    /**
     * JSON 序列化组件。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 支付渠道 Mapper。
     */
    @Autowired
    private PayChannelDOMapper channelDOMapper;

    /**
     * 支付订单 Mapper。
     */
    @Autowired
    private PayOrderDOMapper orderDOMapper;

    /**
     * 支付通知日志 Mapper。
     */
    @Autowired
    private PayNotifyLogDOMapper notifyLogDOMapper;

    /**
     * 查询当前租户下的支付渠道列表。
     *
     * @return 支付渠道记录列表。
     */
    @Override
    public List<DomainRecord> listChannels() {
        // 按当前租户构造渠道查询条件。
        PayChannelDOExample example = new PayChannelDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 按排序号和主键倒序稳定输出。
        example.setOrderByClause("sort_no ASC, id DESC");
        // 查询 BLOB 字段并转换为应用层动态记录。
        return channelDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toChannelRecord)
                .collect(Collectors.toList());
    }

    /**
     * 查询支付渠道详情。
     *
     * @param id 支付渠道标识。
     * @return 支付渠道记录。
     */
    @Override
    public DomainRecord getChannel(String id) {
        // 复用必存在查询，避免返回空记录。
        return toChannelRecord(requireChannel(id));
    }

    /**
     * 创建支付渠道。
     *
     * @param command 支付渠道创建命令。
     * @return 新建支付渠道记录。
     */
    @Override
    public DomainRecord createChannel(Map<String, Object> command) {
        // 创建持久化对象并填充租户与创建时间。
        Date now = new Date();
        PayChannelDO record = new PayChannelDO();
        record.setTenantId(tenantId());
        record.setCreateTime(now);
        // 将命令字段合并到渠道对象。
        applyChannel(record, command);
        record.setUpdateTime(now);
        // 使用 MyBatis Generator 选择性插入。
        channelDOMapper.insertSelective(record);
        return toChannelRecord(record);
    }

    /**
     * 更新支付渠道。
     *
     * @param id 支付渠道标识。
     * @param command 支付渠道更新命令。
     * @return 更新后的支付渠道记录。
     */
    @Override
    public DomainRecord updateChannel(String id, Map<String, Object> command) {
        // 先按租户约束读取原记录。
        PayChannelDO record = requireChannel(id);
        // 合并更新命令并刷新更新时间。
        applyChannel(record, command);
        record.setUpdateTime(new Date());
        // 使用选择性更新避免覆盖未传字段。
        channelDOMapper.updateByPrimaryKeySelective(record);
        // 重新读取一次，返回数据库落库后的记录。
        return toChannelRecord(requireChannel(String.valueOf(record.getId())));
    }

    /**
     * 创建支付订单。
     *
     * @param command 支付订单创建命令。
     * @return 新建支付订单记录。
     */
    @Override
    public DomainRecord createOrder(Map<String, Object> command) {
        // 创建订单持久化对象。
        Date now = new Date();
        PayOrderDO record = new PayOrderDO();
        record.setTenantId(tenantId());
        // 从命令中读取业务字段，兼容驼峰和下划线字段名。
        record.setOrderNo(value(command, "orderNo", value(command, "order_no", "PAY" + System.currentTimeMillis())));
        record.setChannelId(longValue(value(command, "channelId", value(command, "channel_id", "0")), 0L));
        record.setSubject(value(command, "subject", "支付订单"));
        record.setAmount(decimalValue(command.get("amount"), BigDecimal.ZERO));
        record.setCurrency(value(command, "currency", "CNY"));
        record.setStatus(value(command, "status", PayOrderStatus.PAYING.getCode()));
        record.setClientIp(value(command, "clientIp", value(command, "client_ip", "")));
        record.setExtraJson(jsonValue(command.get("extra")));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        // 插入支付订单。
        orderDOMapper.insertSelective(record);
        return toOrderRecord(record);
    }

    /**
     * 查询支付订单详情。
     *
     * @param id 支付订单标识或订单号。
     * @return 支付订单记录。
     */
    @Override
    public DomainRecord getOrder(String id) {
        // 支持按主键或订单号查询订单。
        return toOrderRecord(requireOrder(id));
    }

    /**
     * 查询当前租户下的支付订单列表。
     *
     * @return 支付订单记录列表。
     */
    @Override
    public List<DomainRecord> listOrders() {
        // 按当前租户构造订单查询条件。
        PayOrderDOExample example = new PayOrderDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        // 查询订单列表并转换为应用层记录。
        return orderDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toOrderRecord)
                .collect(Collectors.toList());
    }

    /**
     * 应用支付平台通知。
     *
     * @param orderNo 支付订单号。
     * @param status 通知目标状态。
     * @param payload 通知原始报文。
     * @param signatureValid 签名校验结果。
     * @return 支付通知日志记录。
     */
    @Override
    public DomainRecord applyNotify(String orderNo, String status, String payload, String signatureValid) {
        // 查询订单和渠道上下文。
        PayOrderDO order = requireOrder(orderNo);
        PayChannelDO channel = findChannelById(order.getChannelId());
        Date now = new Date();

        // 先记录通知日志，保留原始通知报文和验签结果。
        PayNotifyLogDO log = new PayNotifyLogDO();
        log.setTenantId(tenantId());
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setProvider(channel == null ? "" : channel.getProvider());
        log.setNotifyId(order.getOrderNo() + "-" + now.getTime());
        log.setSignatureValid(signatureValid);
        log.setStatus(status);
        log.setNotifyPayload(payload);
        log.setCreateTime(now);
        log.setUpdateTime(now);
        notifyLogDOMapper.insertSelective(log);

        // 验签成功才推动订单状态流转，验签失败只留日志。
        if (PayNotifyResult.SUCCESS.matches(signatureValid)) {
            transitOrder(order, PayOrderStatus.fromCode(status), now);
        }
        return toNotifyLogRecord(log);
    }

    /**
     * 查询订单通知日志。
     *
     * @param orderNo 支付订单号。
     * @return 通知日志记录列表。
     */
    @Override
    public List<DomainRecord> listNotifyLogs(String orderNo) {
        // 按租户和订单号查询通知日志。
        PayNotifyLogDOExample example = new PayNotifyLogDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andOrderNoEqualTo(orderNo);
        example.setOrderByClause("create_time DESC, id DESC");
        // 转换为应用层动态记录。
        return notifyLogDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toNotifyLogRecord)
                .collect(Collectors.toList());
    }

    /**
     * 将命令字段合并到支付渠道持久化对象。
     *
     * @param record 支付渠道持久化对象。
     * @param command 支付渠道命令。
     */
    private void applyChannel(PayChannelDO record, Map<String, Object> command) {
        // 读取基础渠道字段，未传字段沿用原值或默认值。
        record.setName(value(command, "name", value(record.getName(), "支付渠道")));
        record.setProvider(value(command, "provider", value(record.getProvider(), "MOCK")));
        record.setAppId(value(command, "appId", value(command, "app_id", value(record.getAppId(), ""))));
        record.setAppSecret(value(command, "appSecret", value(command, "app_secret", value(record.getAppSecret(), ""))));
        record.setStatus(value(command, "status", value(record.getStatus(), "ACTIVE")));
        record.setSortNo(intValue(command.get("sortNo"), intValue(record.getSortNo(), 0)));
        record.setConfigJson(jsonValue(command.containsKey("config") ? command.get("config") : record.getConfigJson()));
    }

    /**
     * 执行支付订单状态流转。
     *
     * @param order 支付订单持久化对象。
     * @param targetStatus 目标订单状态。
     * @param now 本次流转时间。
     */
    private void transitOrder(PayOrderDO order, PayOrderStatus targetStatus, Date now) {
        // 解析当前订单状态。
        PayOrderStatus currentStatus = PayOrderStatus.fromCode(order.getStatus());
        // 状态一致时保持幂等。
        if (currentStatus == targetStatus) {
            return;
        }
        // 支付平台可能直接把 CREATED 推到 PAID，兼容这种通知语义。
        boolean directPaid = currentStatus == PayOrderStatus.CREATED && targetStatus == PayOrderStatus.PAID;
        // 其他状态变更仍按领域枚举校验。
        if (!currentStatus.canTransitTo(targetStatus) && !directPaid) {
            throw new IllegalStateException("非法支付状态流转: "
                    + currentStatus.getCode() + " -> " + targetStatus.getCode());
        }
        // 写入目标状态和支付完成时间。
        order.setStatus(targetStatus.getCode());
        if (targetStatus == PayOrderStatus.PAID) {
            order.setPaidAt(now);
        }
        order.setUpdateTime(now);
        // 持久化订单状态。
        orderDOMapper.updateByPrimaryKeySelective(order);
    }

    /**
     * 查询必须存在的支付渠道。
     *
     * @param id 支付渠道标识。
     * @return 支付渠道持久化对象。
     */
    private PayChannelDO requireChannel(String id) {
        // 按主键查询渠道。
        PayChannelDO record = findChannelById(longValue(id, -1L));
        if (record == null) {
            throw new IllegalArgumentException("支付渠道不存在: " + id);
        }
        return record;
    }

    /**
     * 按主键查询当前租户下的支付渠道。
     *
     * @param id 支付渠道主键。
     * @return 支付渠道持久化对象，未命中时返回 null。
     */
    private PayChannelDO findChannelById(Long id) {
        // 非法主键直接视为未命中。
        if (id == null || id <= 0) {
            return null;
        }
        // 主键查询后再校验租户，避免越权读取。
        PayChannelDO record = channelDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 查询必须存在的支付订单。
     *
     * @param id 支付订单主键或订单号。
     * @return 支付订单持久化对象。
     */
    private PayOrderDO requireOrder(String id) {
        // 优先按主键查询。
        PayOrderDO record = orderDOMapper.selectByPrimaryKey(longValue(id, -1L));
        if (record != null && tenantId().equals(record.getTenantId())) {
            return record;
        }
        // 主键未命中时按订单号查询。
        PayOrderDOExample example = new PayOrderDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andOrderNoEqualTo(id);
        List<PayOrderDO> records = orderDOMapper.selectByExampleWithBLOBs(example);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("支付订单不存在: " + id);
        }
        return records.get(0);
    }

    /**
     * 将支付渠道持久化对象转换为应用层动态记录。
     *
     * @param entity 支付渠道持久化对象。
     * @return 应用层动态记录。
     */
    private DomainRecord toChannelRecord(PayChannelDO entity) {
        // 使用有序 Map 保持接口输出字段稳定。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("name", entity.getName());
        row.put("provider", entity.getProvider());
        row.put("appId", entity.getAppId());
        row.put("appSecret", entity.getAppSecret());
        row.put("status", entity.getStatus());
        row.put("sortNo", entity.getSortNo());
        row.put("config", readJson(entity.getConfigJson()));
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 将支付订单持久化对象转换为应用层动态记录。
     *
     * @param entity 支付订单持久化对象。
     * @return 应用层动态记录。
     */
    private DomainRecord toOrderRecord(PayOrderDO entity) {
        // 组装订单接口字段。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("orderNo", entity.getOrderNo());
        row.put("channelId", stringValue(entity.getChannelId()));
        row.put("subject", entity.getSubject());
        row.put("amount", entity.getAmount());
        row.put("currency", entity.getCurrency());
        row.put("status", entity.getStatus());
        row.put("clientIp", entity.getClientIp());
        row.put("extra", readJson(entity.getExtraJson()));
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        row.put("paidAt", stringValue(entity.getPaidAt()));
        return DomainRecord.of(row);
    }

    /**
     * 将支付通知日志持久化对象转换为应用层动态记录。
     *
     * @param entity 支付通知日志持久化对象。
     * @return 应用层动态记录。
     */
    private DomainRecord toNotifyLogRecord(PayNotifyLogDO entity) {
        // 组装通知日志接口字段。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("orderId", stringValue(entity.getOrderId()));
        row.put("orderNo", entity.getOrderNo());
        row.put("provider", entity.getProvider());
        row.put("notifyId", entity.getNotifyId());
        row.put("signatureValid", entity.getSignatureValid());
        row.put("status", entity.getStatus());
        row.put("payload", entity.getNotifyPayload());
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 读取 JSON 字符串。
     *
     * @param json JSON 字符串。
     * @return JSON 节点。
     */
    private JsonNode readJson(String json) {
        try {
            // 空值统一按空对象处理，减少前端判空。
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 解析失败", ex);
        }
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 原始对象。
     * @return JSON 字符串。
     */
    private String jsonValue(Object value) {
        try {
            // 空值统一保存为空对象。
            if (value == null) {
                return "{}";
            }
            // 字符串视为已经序列化的 JSON。
            if (value instanceof String) {
                String text = ((String) value).trim();
                return StringUtils.isBlank(text) ? "{}" : text;
            }
            // 其他对象交给 Jackson 序列化。
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 序列化失败", ex);
        }
    }

    /**
     * 从命令中读取字符串字段。
     *
     * @param command 命令对象。
     * @param key 字段名。
     * @param defaultValue 默认值。
     * @return 字符串字段值。
     */
    private String value(Map<String, Object> command, String key, String defaultValue) {
        return command.containsKey(key) ? value(command.get(key), defaultValue) : defaultValue;
    }

    /**
     * 将对象转换为非空字符串。
     *
     * @param raw 原始值。
     * @param defaultValue 默认值。
     * @return 字符串值。
     */
    private String value(Object raw, String defaultValue) {
        String text = stringValue(raw);
        return StringUtils.isBlank(text) ? defaultValue : text;
    }

    /**
     * 将对象转换为整数。
     *
     * @param raw 原始值。
     * @param defaultValue 默认值。
     * @return 整数值。
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
     * 将对象转换为长整型。
     *
     * @param raw 原始值。
     * @param defaultValue 默认值。
     * @return 长整型值。
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
     * 将对象转换为金额数值。
     *
     * @param raw 原始值。
     * @param defaultValue 默认值。
     * @return 金额数值。
     */
    private BigDecimal decimalValue(Object raw, BigDecimal defaultValue) {
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        if (raw instanceof Number) {
            return BigDecimal.valueOf(((Number) raw).doubleValue());
        }
        try {
            return raw == null ? defaultValue : new BigDecimal(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为去空白字符串。
     *
     * @param value 原始值。
     * @return 字符串值。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 获取当前租户标识。
     *
     * @return 当前租户标识，未绑定租户时使用默认租户。
     */
    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
