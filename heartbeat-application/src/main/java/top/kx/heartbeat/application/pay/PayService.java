package top.kx.heartbeat.application.pay;


import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.domain.pay.PayNotifyResult;
import top.kx.heartbeat.domain.pay.PayNotifyStatus;
import top.kx.heartbeat.domain.pay.PayOrderStatus;
import top.kx.heartbeat.application.pay.port.PayRepository;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付应用服务。
 *
 * <p>负责编排支付渠道、支付订单、支付通知和签名能力。</p>
 */
@Service
public class PayService {

    /**
     * 支付签名算法。
     */
    private static final String SIGN_ALGORITHM = "HmacSHA256";

    /**
     * 支付仓储。
     */
    @Resource
    private PayRepository payRepository;

    /**
     * 查询支付渠道列表。
     *
     * @return 支付渠道列表。
     */
    public List<Map<String, Object>> listChannels() {
        // 查询支付渠道领域记录并转换为字段 Map 列表。
        return maps(payRepository.listChannels());
    }

    /**
     * 查询支付渠道详情。
     *
     * @param id 支付渠道标识。
     * @return 支付渠道详情。
     */
    public Map<String, Object> getChannel(String id) {
        // 复制支付渠道字段，避免直接修改领域记录副本之外的数据。
        Map<String, Object> channel = new LinkedHashMap<>(payRepository.getChannel(id).toMap());
        // 脱敏支付渠道密钥。
        channel.put("appSecret", mask(stringValue(channel.get("appSecret"))));
        // 返回脱敏后的支付渠道详情。
        return channel;
    }

    /**
     * 创建支付渠道。
     *
     * @param command 支付渠道创建命令。
     * @return 新建支付渠道。
     */
    @Transactional
    public Map<String, Object> createChannel(Map<String, Object> command) {
        // 委托仓储创建支付渠道并返回字段 Map。
        return payRepository.createChannel(command).toMap();
    }

    /**
     * 更新支付渠道。
     *
     * @param id 支付渠道标识。
     * @param command 支付渠道更新命令。
     * @return 更新后的支付渠道。
     */
    @Transactional
    public Map<String, Object> updateChannel(String id, Map<String, Object> command) {
        // 委托仓储更新支付渠道并返回字段 Map。
        return payRepository.updateChannel(id, command).toMap();
    }

    /**
     * 创建支付订单。
     *
     * @param command 支付订单创建命令。
     * @return 新建支付订单。
     */
    @Transactional
    public Map<String, Object> createOrder(Map<String, Object> command) {
        // 委托仓储创建支付订单并返回字段 Map。
        return payRepository.createOrder(command).toMap();
    }

    /**
     * 查询支付订单详情。
     *
     * @param id 支付订单标识。
     * @return 支付订单详情。
     */
    public Map<String, Object> getOrder(String id) {
        // 委托仓储查询支付订单并返回字段 Map。
        return payRepository.getOrder(id).toMap();
    }

    /**
     * 查询支付订单列表。
     *
     * @return 支付订单列表。
     */
    public List<Map<String, Object>> listOrders() {
        // 查询支付订单领域记录并转换为字段 Map 列表。
        return maps(payRepository.listOrders());
    }

    /**
     * 查询支付通知日志列表。
     *
     * @param orderNo 支付订单号。
     * @return 支付通知日志列表。
     */
    public List<Map<String, Object>> listNotifyLogs(String orderNo) {
        // 查询支付通知日志领域记录并转换为字段 Map 列表。
        return maps(payRepository.listNotifyLogs(orderNo));
    }

    /**
     * 处理支付平台通知。
     *
     * @param orderNo 支付订单号。
     * @param command 支付通知命令。
     * @return 支付通知处理结果。
     */
    @Transactional
    public Map<String, Object> handleNotify(String orderNo, Map<String, Object> command) {
        // 查询支付订单上下文。
        Map<String, Object> order = payRepository.getOrder(orderNo).toMap();
        // 查询支付渠道上下文。
        Map<String, Object> channel = payRepository.getChannel(stringValue(order.get("channelId"))).toMap();
        // 读取支付平台通知原文。
        String payload = stringValue(command.get("payload"));
        // 读取支付渠道签名密钥。
        String secret = stringValue(channel.get("appSecret"));
        // 读取支付平台回传签名。
        String signature = stringValue(command.get("signature"));
        // 计算服务端期望签名。
        String expected = sign(payload, secret);
        // 用枚举表达验签结果，避免 SUCCESS/FAIL 字符串散落。
        PayNotifyResult notifyResult = expected.equals(signature) ? PayNotifyResult.SUCCESS : PayNotifyResult.FAIL;
        // 验签成功时使用支付平台回传状态，未回传时默认流转到已支付。
        String successStatus = stringValue(command.get("status"), PayOrderStatus.PAID.getCode());
        // 验签失败只记录通知失败状态，不推动订单状态流转。
        String notifyStatus = notifyResult == PayNotifyResult.SUCCESS ? successStatus : PayNotifyStatus.SIGN_FAIL.getCode();
        // 统一交给仓储处理通知幂等和订单状态机。
        return payRepository.applyNotify(orderNo, notifyStatus, payload, notifyResult.getCode()).toMap();
    }

    /**
     * 生成支付通知签名。
     *
     * @param payload 签名原文。
     * @param secret 签名密钥。
     * @return 签名结果。
     */
    public String sign(String payload, String secret) {
        // 捕获底层加密异常并转换为业务参数异常。
        try {
            // 创建 HMAC 签名器。
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            // 使用 UTF-8 密钥初始化签名器。
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            // 对通知原文执行签名。
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            // 创建十六进制签名结果构造器。
            StringBuilder result = new StringBuilder();
            // 遍历签名字节数组。
            for (byte value : bytes) {
                // 追加两位十六进制字符。
                result.append(String.format("%02x", value));
            }
            // 返回十六进制签名字符串。
            return result.toString();
        } catch (Exception ex) {
            // 签名生成失败时抛出业务参数异常。
            throw new IllegalArgumentException("支付签名生成失败", ex);
        }
    }

    /**
     * 脱敏敏感字符串。
     *
     * @param value 原始字符串。
     * @return 脱敏字符串。
     */
    private String mask(String value) {
        // 空值直接返回空字符串。
        if (StringUtils.isEmpty(value)) {
            // 返回空字符串。
            return "";
        }
        // 长度不超过四位时全量掩码。
        if (value.length() <= 4) {
            // 返回固定掩码。
            return "****";
        }
        // 保留前后两位，中间使用掩码。
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /**
     * 将对象转换为字符串。
     *
     * @param value 原始值。
     * @return 字符串。
     */
    private String stringValue(Object value) {
        // 使用空字符串作为默认值。
        return stringValue(value, "");
    }

    /**
     * 将对象转换为字符串并支持默认值。
     *
     * @param value 原始值。
     * @param defaultValue 默认值。
     * @return 字符串。
     */
    private String stringValue(Object value, String defaultValue) {
        // 将原始值转换为去空白字符串。
        String text = value == null ? "" : String.valueOf(value).trim();
        // 字符串为空时返回默认值。
        return StringUtils.isEmpty(text) ? defaultValue : text;
    }

    /**
     * 将领域记录列表转换为字段 Map 列表。
     *
     * @param records 领域记录列表。
     * @return 字段 Map 列表。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 逐条导出领域记录的字段副本。
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }
}
