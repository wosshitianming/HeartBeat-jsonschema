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
import java.util.stream.Collectors;

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
        return payChannelRepository.listChannels().stream()
                .map(this::maskedChannel)
                .collect(Collectors.toList());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，协调支付业务相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    public RecordResponse getChannel(String id) {
        return maskedChannel(payChannelRepository.getChannel(id));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调支付业务相关仓储和领域规则。
     *
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createChannel(PayChannelRequest request) {
        return maskedChannel(payChannelRepository.createChannel(request));
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
        return maskedChannel(payChannelRepository.updateChannel(id, request));
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
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PayNotifyRequest safeRequest = request == null ? new PayNotifyRequest() : request;
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        Map<String, Object> order = payOrderRepository.getOrder(orderNo).toMap();
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        Map<String, Object> channel = payChannelRepository.getChannel(stringValue(order.get("channelId"))).toMap();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String payload = stringValue(safeRequest.getPayload());
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String secret = stringValue(channel.get("appSecret"));
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String signature = stringValue(safeRequest.getSignature());
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String expected = sign(payload, secret);
        // 比较本地签名和回调签名，得到本次通知的验签结果。
        PayNotifyResult notifyResult = expected.equals(signature) ? PayNotifyResult.SUCCESS : PayNotifyResult.FAIL;
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String successStatus = stringValue(safeRequest.getStatus(), PayOrderStatus.PAID.getCode());
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String notifyStatus = notifyResult == PayNotifyResult.SUCCESS
                // 条件成立时使用前一个分支计算出的业务值。
                ? successStatus
                // 条件不成立时使用兜底业务值。
                : PayNotifyStatus.SIGN_FAIL.getCode();

        // 创建下游写入请求对象，集中承载本次业务处理结果。
        PayNotifyLogRequest logRequest = new PayNotifyLogRequest();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        logRequest.setOrderId(stringValue(order.get("id")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        logRequest.setOrderNo(stringValue(order.get("orderNo")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        logRequest.setProvider(stringValue(channel.get("provider")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        logRequest.setPayload(payload);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        logRequest.setStatus(notifyStatus);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        logRequest.setSignatureValid(notifyResult.getCode());
        // 将持久化结果转换为统一响应对象，保持接口返回口径一致。
        RecordResponse log = RecordResponse.from(payNotifyLogRepository.recordNotify(logRequest));

        // 根据当前业务条件选择对应处理路径。
        if (notifyResult == PayNotifyResult.SUCCESS) {
            // 将当前业务变更写入持久化层，保持数据状态同步。
            payOrderRepository.updateOrderStatus(orderNo, successStatus);
        }
        // 返回已经完成封装的业务结果。
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
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 按签名算法处理字节数据，保证验签结果可重复计算。
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            // 按签名算法处理字节数据，保证验签结果可重复计算。
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            // 按签名算法处理字节数据，保证验签结果可重复计算。
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            StringBuilder result = new StringBuilder();
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (byte value : bytes) {
                // 按签名算法处理字节数据，保证验签结果可重复计算。
                result.append(String.format("%02x", value));
            }
            // 返回已经完成封装的业务结果。
            return result.toString();
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
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
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isEmpty(value)) {
            // 返回已经完成封装的业务结果。
            return "";
        }
        // 根据当前业务条件选择对应处理路径。
        return "******";
    }

    private RecordResponse maskedChannel(DomainRecord record) {
        Map<String, Object> channel = new LinkedHashMap<>(record.toMap());
        channel.put("appSecret", mask(stringValue(channel.get("appSecret"))));
        return RecordResponse.from(channel);
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
