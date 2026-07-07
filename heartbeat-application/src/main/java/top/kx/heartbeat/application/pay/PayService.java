// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Service
public class PayService {

    // 注释：声明当前成员或方法。
    private static final String SIGN_ALGORITHM = "HmacSHA256";

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PayChannelRepository payChannelRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PayOrderRepository payOrderRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PayNotifyLogRepository payNotifyLogRepository;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listChannels() {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(payChannelRepository.listChannels()));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse getChannel(String id) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> channel = new LinkedHashMap<>(payChannelRepository.getChannel(id).toMap());
        // 注释：执行当前代码行。
        channel.put("appSecret", mask(stringValue(channel.get("appSecret"))));
        // 注释：返回当前处理结果。
        return RecordResponse.from(channel);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse createChannel(PayChannelRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(payChannelRepository.createChannel(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse updateChannel(String id, PayChannelRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(payChannelRepository.updateChannel(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse createOrder(PayOrderRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(payOrderRepository.createOrder(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse getOrder(String id) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(payOrderRepository.getOrder(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listOrders() {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(payOrderRepository.listOrders()));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listNotifyLogs(String orderNo) {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(payNotifyLogRepository.listNotifyLogs(orderNo)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse handleNotify(String orderNo, PayNotifyRequest request) {
        // 注释：设置或计算当前变量值。
        PayNotifyRequest safeRequest = request == null ? new PayNotifyRequest() : request;
        // 注释：设置或计算当前变量值。
        Map<String, Object> order = payOrderRepository.getOrder(orderNo).toMap();
        // 注释：设置或计算当前变量值。
        Map<String, Object> channel = payChannelRepository.getChannel(stringValue(order.get("channelId"))).toMap();
        // 注释：设置或计算当前变量值。
        String payload = stringValue(safeRequest.getPayload());
        // 注释：设置或计算当前变量值。
        String secret = stringValue(channel.get("appSecret"));
        // 注释：设置或计算当前变量值。
        String signature = stringValue(safeRequest.getSignature());
        // 注释：设置或计算当前变量值。
        String expected = sign(payload, secret);
        // 注释：设置或计算当前变量值。
        PayNotifyResult notifyResult = expected.equals(signature) ? PayNotifyResult.SUCCESS : PayNotifyResult.FAIL;
        // 注释：设置或计算当前变量值。
        String successStatus = stringValue(safeRequest.getStatus(), PayOrderStatus.PAID.getCode());
        // 注释：设置或计算当前变量值。
        String notifyStatus = notifyResult == PayNotifyResult.SUCCESS
                // 注释：执行当前代码行。
                ? successStatus
                // 注释：执行当前代码行。
                : PayNotifyStatus.SIGN_FAIL.getCode();

        // 注释：设置或计算当前变量值。
        PayNotifyLogRequest logRequest = new PayNotifyLogRequest();
        // 注释：执行当前代码行。
        logRequest.setOrderId(stringValue(order.get("id")));
        // 注释：执行当前代码行。
        logRequest.setOrderNo(stringValue(order.get("orderNo")));
        // 注释：执行当前代码行。
        logRequest.setProvider(stringValue(channel.get("provider")));
        // 注释：执行当前代码行。
        logRequest.setPayload(payload);
        // 注释：执行当前代码行。
        logRequest.setStatus(notifyStatus);
        // 注释：执行当前代码行。
        logRequest.setSignatureValid(notifyResult.getCode());
        // 注释：设置或计算当前变量值。
        RecordResponse log = RecordResponse.from(payNotifyLogRepository.recordNotify(logRequest));

        // 注释：判断当前业务条件。
        if (notifyResult == PayNotifyResult.SUCCESS) {
            // 注释：执行当前代码行。
            payOrderRepository.updateOrderStatus(orderNo, successStatus);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return log;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public String sign(String payload, String secret) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：设置或计算当前变量值。
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            // 注释：执行当前代码行。
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            // 注释：设置或计算当前变量值。
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            // 注释：设置或计算当前变量值。
            StringBuilder result = new StringBuilder();
            // 注释：遍历当前数据集合。
            for (byte value : bytes) {
                // 注释：执行当前代码行。
                result.append(String.format("%02x", value));
                // 注释：结束当前代码块。
            }
            // 注释：返回当前处理结果。
            return result.toString();
            // 注释：捕获并处理当前异常。
        } catch (Exception ex) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Pay signature generation failed", ex);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String mask(String value) {
        // 注释：判断当前业务条件。
        if (StringUtils.isEmpty(value)) {
            // 注释：返回当前处理结果。
            return "";
            // 注释：结束当前代码块。
        }
        // 注释：判断当前业务条件。
        if (value.length() <= 4) {
            // 注释：返回当前处理结果。
            return "****";
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String stringValue(Object value) {
        // 注释：返回当前处理结果。
        return stringValue(value, "");
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String stringValue(Object value, String defaultValue) {
        // 注释：设置或计算当前变量值。
        String text = value == null ? "" : String.valueOf(value).trim();
        // 注释：返回当前处理结果。
        return StringUtils.isEmpty(text) ? defaultValue : text;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 注释：返回当前处理结果。
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
