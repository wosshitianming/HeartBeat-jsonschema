package top.kx.heartbeat.application.pay;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.pay.port.PayChannelRepository;
import top.kx.heartbeat.application.pay.port.PayNotifyLogRepository;
import top.kx.heartbeat.application.pay.port.PayOrderRepository;
import top.kx.heartbeat.application.pay.request.PayChannelRequest;
import top.kx.heartbeat.application.pay.request.PayNotifyLogRequest;
import top.kx.heartbeat.application.pay.request.PayNotifyRequest;
import top.kx.heartbeat.application.pay.request.PayOrderRequest;
import top.kx.heartbeat.domain.pay.PayNotifyResult;
import top.kx.heartbeat.domain.pay.PayNotifyStatus;
import top.kx.heartbeat.domain.pay.PayOrderStatus;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 编排支付业务应用用例，承接接口层请求并协调仓储与领域能力。
 */
@Service
public class PayService {

    private static final String SIGN_ALGORITHM = "HmacSHA256";

    @Resource
    private PayChannelRepository payChannelRepository;
    @Resource
    private PayOrderRepository payOrderRepository;
    @Resource
    private PayNotifyLogRepository payNotifyLogRepository;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调支付业务相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listChannels() {
        return RecordResponse.fromMaps(maps(payChannelRepository.listChannels()));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，协调支付业务相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    public RecordResponse getChannel(String id) {
        Map<String, Object> channel = new LinkedHashMap<>(payChannelRepository.getChannel(id).toMap());
        channel.put("appSecret", mask(stringValue(channel.get("appSecret"))));
        return RecordResponse.from(channel);
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调支付业务相关仓储和领域规则。
     *
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createChannel(PayChannelRequest request) {
        return RecordResponse.from(payChannelRepository.createChannel(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，协调支付业务相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse updateChannel(String id, PayChannelRequest request) {
        return RecordResponse.from(payChannelRepository.updateChannel(id, request));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调支付业务相关仓储和领域规则。
     *
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createOrder(PayOrderRequest request) {
        return RecordResponse.from(payOrderRepository.createOrder(request));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，协调支付业务相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    public RecordResponse getOrder(String id) {
        return RecordResponse.from(payOrderRepository.getOrder(id));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调支付业务相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listOrders() {
        return RecordResponse.fromMaps(maps(payOrderRepository.listOrders()));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调支付业务相关仓储和领域规则。
     *
     * @param orderNo 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listNotifyLogs(String orderNo) {
        return RecordResponse.fromMaps(maps(payNotifyLogRepository.listNotifyLogs(orderNo)));
    }

    /**
     * 处理支付渠道回调，完成验签、通知日志记录和订单状态推进，协调支付业务相关仓储和领域规则。
     *
     * @param orderNo 业务处理所需参数。
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse handleNotify(String orderNo, PayNotifyRequest request) {
        PayNotifyRequest safeRequest = request == null ? new PayNotifyRequest() : request;
        Map<String, Object> order = payOrderRepository.getOrder(orderNo).toMap();
        Map<String, Object> channel = payChannelRepository.getChannel(stringValue(order.get("channelId"))).toMap();
        String payload = stringValue(safeRequest.getPayload());
        String secret = stringValue(channel.get("appSecret"));
        String signature = stringValue(safeRequest.getSignature());
        String expected = sign(payload, secret);
        PayNotifyResult notifyResult = expected.equals(signature) ? PayNotifyResult.SUCCESS : PayNotifyResult.FAIL;
        String successStatus = stringValue(safeRequest.getStatus(), PayOrderStatus.PAID.getCode());
        String notifyStatus = notifyResult == PayNotifyResult.SUCCESS
                ? successStatus
                : PayNotifyStatus.SIGN_FAIL.getCode();

        PayNotifyLogRequest logRequest = new PayNotifyLogRequest();
        logRequest.setOrderId(stringValue(order.get("id")));
        logRequest.setOrderNo(stringValue(order.get("orderNo")));
        logRequest.setProvider(stringValue(channel.get("provider")));
        logRequest.setPayload(payload);
        logRequest.setStatus(notifyStatus);
        logRequest.setSignatureValid(notifyResult.getCode());
        RecordResponse log = RecordResponse.from(payNotifyLogRepository.recordNotify(logRequest));

        if (notifyResult == PayNotifyResult.SUCCESS) {
            payOrderRepository.updateOrderStatus(orderNo, successStatus);
        }
        return log;
    }

    /**
     * 根据渠道密钥生成 HMAC 签名，用于校验支付回调来源，协调支付业务相关仓储和领域规则。
     *
     * @param payload 支付渠道回调原文。
     * @param secret 渠道签名密钥。
     * @return 处理后的业务结果。
     */
    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Pay signature generation failed", ex);
        }
    }

    /**
     * 脱敏敏感配置，避免密钥在接口响应中明文暴露，协调支付业务相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String mask(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，协调支付业务相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        return stringValue(value, "");
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，协调支付业务相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value, String defaultValue) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return StringUtils.isEmpty(text) ? defaultValue : text;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调支付业务相关仓储和领域规则。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }
}
