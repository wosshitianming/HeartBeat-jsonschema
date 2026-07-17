package top.kx.heartbeat.application.flow.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 为运行账本生成不会重复持久化大正文的 payload 摘要。
 */
@Service
public class FlowPayloadSummaryService {

    public static final String SUMMARY_FORMAT = "HB_FLOW_PAYLOAD_SUMMARY_V1";

    @Resource
    private ObjectMapper objectMapper;

    @Value("${heartbeat.flow.flowable.inline-payload-max-bytes:65536}")
    private int inlinePayloadMaxBytes;

    public Map<String, Object> summarize(Map<String, Object> payload) {
        Map<String, Object> safePayload = payload == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        String json = toJson(safePayload);
        int bytes = json.getBytes(StandardCharsets.UTF_8).length;
        if (bytes <= inlinePayloadMaxBytes) return safePayload;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("payloadSummary", SUMMARY_FORMAT);
        summary.put("slimmed", true);
        summary.put("bytes", bytes);
        summary.put("fieldCount", safePayload.size());
        summary.put("fields", new ArrayList<>(safePayload.keySet()).subList(
                0, Math.min(safePayload.size(), 32)));
        summary.put("payloadSha256", sha256(json));
        return summary;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Flow payload 摘要序列化失败", ex);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item & 0xff));
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Flow payload 摘要计算失败", ex);
        }
    }
}
