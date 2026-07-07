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

@Service
public class PayService {

    private static final String SIGN_ALGORITHM = "HmacSHA256";

    @Resource
    private PayChannelRepository payChannelRepository;
    @Resource
    private PayOrderRepository payOrderRepository;
    @Resource
    private PayNotifyLogRepository payNotifyLogRepository;

    public List<RecordResponse> listChannels() {
        return RecordResponse.fromMaps(maps(payChannelRepository.listChannels()));
    }

    public RecordResponse getChannel(String id) {
        Map<String, Object> channel = new LinkedHashMap<>(payChannelRepository.getChannel(id).toMap());
        channel.put("appSecret", mask(stringValue(channel.get("appSecret"))));
        return RecordResponse.from(channel);
    }

    @Transactional
    public RecordResponse createChannel(PayChannelRequest request) {
        return RecordResponse.from(payChannelRepository.createChannel(request));
    }

    @Transactional
    public RecordResponse updateChannel(String id, PayChannelRequest request) {
        return RecordResponse.from(payChannelRepository.updateChannel(id, request));
    }

    @Transactional
    public RecordResponse createOrder(PayOrderRequest request) {
        return RecordResponse.from(payOrderRepository.createOrder(request));
    }

    public RecordResponse getOrder(String id) {
        return RecordResponse.from(payOrderRepository.getOrder(id));
    }

    public List<RecordResponse> listOrders() {
        return RecordResponse.fromMaps(maps(payOrderRepository.listOrders()));
    }

    public List<RecordResponse> listNotifyLogs(String orderNo) {
        return RecordResponse.fromMaps(maps(payNotifyLogRepository.listNotifyLogs(orderNo)));
    }

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

    private String mask(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private String stringValue(Object value) {
        return stringValue(value, "");
    }

    private String stringValue(Object value, String defaultValue) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return StringUtils.isEmpty(text) ? defaultValue : text;
    }

    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }
}
