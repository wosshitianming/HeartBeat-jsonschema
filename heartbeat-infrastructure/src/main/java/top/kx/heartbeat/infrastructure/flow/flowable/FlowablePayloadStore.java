package top.kx.heartbeat.infrastructure.flow.flowable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Flowable Payload 瘦身存储。
 *
 * <p>用于避免把大 payload 直接写入 Flowable 变量表，后续可替换为真实持久化表。</p>
 */
@Service
public class FlowablePayloadStore {

    /**
     * JSON 序列化器。
     */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 内联 payload 最大字节数。
     */
    @Value("${heartbeat.flow.flowable.inline-payload-max-bytes:65536}")
    private int inlinePayloadMaxBytes;

    /**
     * 将 payload 转换为 Flowable 变量安全载荷。
     *
     * @param payload 原始 payload
     * @return Flowable 变量安全载荷
     */
    Map<String, Object> slim(Map<String, Object> payload) {
        // 创建安全 payload。
        Map<String, Object> safePayload = payload == null ? new LinkedHashMap<>() : payload;
        // 计算 JSON 字节长度。
        int byteLength = toJson(safePayload).getBytes(StandardCharsets.UTF_8).length;
        // 判断是否可以内联保存。
        if (byteLength <= inlinePayloadMaxBytes) {
            // 返回原始 payload。
            return safePayload;
        }
        // 创建 payload 引用。
        Map<String, Object> ref = new LinkedHashMap<>();
        // 写入引用标识。
        ref.put("payloadRef", UUID.randomUUID().toString());
        // 写入瘦身标记。
        ref.put("slimmed", true);
        // 写入原始字节数。
        ref.put("bytes", byteLength);
        // 返回 payload 引用。
        return ref;
    }

    /**
     * 将 payload 序列化为 JSON。
     *
     * @param payload payload 数据
     * @return JSON 字符串
     */
    private String toJson(Map<String, Object> payload) {
        try {
            // 序列化 payload。
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            // 抛出 payload 序列化异常。
            throw new IllegalArgumentException("Flowable payload 序列化失败", ex);
        }
    }
}
