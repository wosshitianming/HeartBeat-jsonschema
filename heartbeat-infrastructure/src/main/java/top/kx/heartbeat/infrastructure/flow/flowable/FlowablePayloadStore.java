package top.kx.heartbeat.infrastructure.flow.flowable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.infrastructure.security.SecretCryptoService;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flowable Payload 瘦身存储。
 *
 * <p>大 payload 加密写入 {@code hb_flow_payload}，Flowable 变量只保存租户隔离的引用。</p>
 */
@Service
public class FlowablePayloadStore {

    private static final String STORAGE_FORMAT = "HB_FLOW_PAYLOAD_AES_GCM_V1";
    private static final String INLINE_FORMAT = "HB_FLOW_PAYLOAD_INLINE_V1";
    private static final String STORAGE_KEY = "payloadStorage";
    private static final String CIPHER_FORMAT = "AES_GCM_V1";
    private static final String TOKEN_PREFIX = "{aes-gcm}";

    /**
     * JSON 序列化器。
     */
    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private SecretCryptoService secretCryptoService;

    /**
     * 内联 payload 最大字节数。
     */
    @Value("${heartbeat.flow.flowable.inline-payload-max-bytes:65536}")
    private int inlinePayloadMaxBytes;

    /**
     * 将 payload 转换为 Flowable 变量安全载荷。
     *
     * @param payload 原始 payload
     * @param tenantId 租户标识
     * @param runId 运行标识
     * @return Flowable 变量安全载荷
     */
    Map<String, Object> slim(Map<String, Object> payload, String tenantId, String runId) {
        // 创建安全 payload。
        String sourceJson = toJson(payload == null ? new LinkedHashMap<>() : payload);
        Map<String, Object> safePayload = fromJson(sourceJson);
        String payloadJson = toJson(safePayload);
        // 计算 JSON 字节长度。
        int byteLength = payloadJson.getBytes(StandardCharsets.UTF_8).length;
        // 判断是否可以内联保存。
        if (byteLength <= inlinePayloadMaxBytes) {
            return inlineEnvelope(safePayload);
        }
        long tenant = requiredPositiveLong(tenantId, "tenantId");
        long run = requiredPositiveLong(runId, "runId");
        String payloadSha256 = sha256(payloadJson);
        Long payloadId = existingPayloadId(tenant, run, payloadSha256);
        if (payloadId == null) {
            payloadId = insertPayload(tenant, run, payloadSha256, encryptedEnvelope(payloadJson));
        }
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put(STORAGE_KEY, STORAGE_FORMAT);
        ref.put("payloadRef", String.valueOf(payloadId));
        // 写入瘦身标记。
        ref.put("slimmed", true);
        // 写入原始字节数。
        ref.put("bytes", byteLength);
        ref.put("payloadSha256", payloadSha256);
        ref.put("payloadToken", referenceToken(tenant, run, payloadId, payloadSha256));
        // 返回 payload 引用。
        return ref;
    }

    /**
     * 将 Flowable 变量中的 payload 引用还原为原始业务对象。
     */
    Map<String, Object> restore(Map<String, Object> stored, String tenantId, String runId) {
        Map<String, Object> safeStored = stored == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(stored);
        if (isInline(safeStored)) {
            return inlinePayload(safeStored);
        }
        if (!isReference(safeStored)) {
            if (Boolean.parseBoolean(String.valueOf(safeStored.get("slimmed")))
                    && safeStored.containsKey("payloadRef")) {
                throw new IllegalStateException("Flowable payload 使用不可恢复的旧版引用格式");
            }
            return safeStored;
        }
        requireReferenceFields(safeStored);
        long tenant = requiredPositiveLong(tenantId, "tenantId");
        long run = requiredPositiveLong(runId, "runId");
        long payloadId = requiredPositiveLong(String.valueOf(safeStored.get("payloadRef")), "payloadRef");
        String referenceSha256 = String.valueOf(safeStored.get("payloadSha256")).trim();
        validateReferenceToken(safeStored, tenant, run, payloadId, referenceSha256);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT run_id, payload_sha256, payload_json FROM hb_flow_payload WHERE id = ? AND tenant_id = ?",
                payloadId, tenant);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Flowable payload 引用不存在或不属于当前租户: " + payloadId);
        }
        Map<String, Object> row = rows.get(0);
        long storedRun = number(row.get("run_id"));
        if (storedRun != run) {
            throw new IllegalStateException("Flowable payload 引用不属于当前运行: " + payloadId);
        }
        String payloadJson = decryptEnvelope(databaseText(row.get("payload_json")));
        String actualSha256 = sha256(payloadJson);
        String storedSha256 = String.valueOf(row.get("payload_sha256")).trim();
        if (!actualSha256.equalsIgnoreCase(storedSha256)
                || !actualSha256.equalsIgnoreCase(referenceSha256)
                || payloadJson.getBytes(StandardCharsets.UTF_8).length != referenceBytes(safeStored)) {
            throw new IllegalStateException("Flowable payload 完整性校验失败: " + payloadId);
        }
        return fromJson(payloadJson);
    }

    private boolean isReference(Map<String, Object> payload) {
        return STORAGE_FORMAT.equals(String.valueOf(payload.get(STORAGE_KEY)));
    }

    private boolean isInline(Map<String, Object> payload) {
        return INLINE_FORMAT.equals(String.valueOf(payload.get(STORAGE_KEY)));
    }

    Map<String, Object> projectionValue(Map<String, Object> stored) {
        Map<String, Object> safeStored = stored == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(stored);
        if (isInline(safeStored)) return inlinePayload(safeStored);
        if (!isReference(safeStored)) return safeStored;
        requireReferenceFields(safeStored);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put(STORAGE_KEY, STORAGE_FORMAT);
        summary.put("payloadRef", String.valueOf(safeStored.get("payloadRef")));
        summary.put("slimmed", true);
        summary.put("bytes", referenceBytes(safeStored));
        summary.put("payloadSha256", String.valueOf(safeStored.get("payloadSha256")));
        return summary;
    }

    String referenceId(Map<String, Object> stored) {
        if (!isReference(stored)) return null;
        requireReferenceFields(stored);
        return String.valueOf(requiredPositiveLong(String.valueOf(stored.get("payloadRef")), "payloadRef"));
    }

    private Map<String, Object> inlineEnvelope(Map<String, Object> payload) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put(STORAGE_KEY, INLINE_FORMAT);
        envelope.put("payload", new LinkedHashMap<>(payload));
        return envelope;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> inlinePayload(Map<String, Object> envelope) {
        Object value = envelope.get("payload");
        if (!(value instanceof Map)) {
            throw new IllegalStateException("Flowable 内联 payload 格式无效");
        }
        return new LinkedHashMap<>((Map<String, Object>) value);
    }

    private void requireReferenceFields(Map<String, Object> reference) {
        if (reference == null
                || reference.get("payloadRef") == null
                || reference.get("payloadSha256") == null
                || reference.get("payloadToken") == null
                || reference.get("bytes") == null) {
            throw new IllegalStateException("Flowable payload 引用元数据不完整");
        }
        String sha256 = String.valueOf(reference.get("payloadSha256")).trim();
        if (!sha256.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalStateException("Flowable payload 引用摘要格式无效");
        }
        referenceBytes(reference);
    }

    private int referenceBytes(Map<String, Object> reference) {
        try {
            int bytes = Integer.parseInt(String.valueOf(reference.get("bytes")));
            if (bytes > 0) return bytes;
        } catch (NumberFormatException ignored) {
            // 统一在下方抛出元数据异常。
        }
        throw new IllegalStateException("Flowable payload 引用字节数无效");
    }

    private String referenceToken(long tenantId, long runId, long payloadId, String payloadSha256) {
        return secretCryptoService.encryptIfPlain(referenceTokenValue(tenantId, runId, payloadId, payloadSha256));
    }

    private void validateReferenceToken(Map<String, Object> reference, long tenantId, long runId,
                                        long payloadId, String payloadSha256) {
        String token = String.valueOf(reference.get("payloadToken"));
        if (!token.startsWith(TOKEN_PREFIX)) {
            throw new IllegalStateException("Flowable payload 引用令牌格式无效");
        }
        String expected = referenceTokenValue(tenantId, runId, payloadId, payloadSha256);
        String actual = secretCryptoService.decryptIfCipher(token);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalStateException("Flowable payload 引用令牌校验失败");
        }
    }

    private String referenceTokenValue(long tenantId, long runId, long payloadId, String payloadSha256) {
        return tenantId + ":" + runId + ":" + payloadId + ":" + payloadSha256;
    }

    private Long existingPayloadId(long tenantId, long runId, String payloadSha256) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM hb_flow_payload WHERE tenant_id = ? AND run_id = ? AND payload_sha256 = ? "
                        + "ORDER BY id DESC LIMIT 1",
                (resultSet, rowNum) -> resultSet.getLong("id"), tenantId, runId, payloadSha256);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private long insertPayload(long tenantId, long runId, String payloadSha256, String payloadJson) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Instant now = Instant.now();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO hb_flow_payload "
                            + "(tenant_id, run_id, payload_sha256, payload_json, create_time, update_time, create_by, update_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, tenantId);
            statement.setLong(2, runId);
            statement.setString(3, payloadSha256);
            statement.setString(4, payloadJson);
            statement.setTimestamp(5, Timestamp.from(now));
            statement.setTimestamp(6, Timestamp.from(now));
            statement.setLong(7, 0L);
            statement.setLong(8, 0L);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Flowable payload 持久化未返回主键");
        }
        return key.longValue();
    }

    private String encryptedEnvelope(String payloadJson) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("format", CIPHER_FORMAT);
        envelope.put("ciphertext", secretCryptoService.encryptIfPlain(payloadJson));
        return toJson(envelope);
    }

    private String decryptEnvelope(String storedJson) {
        Map<String, Object> envelope = fromJson(storedJson);
        if (!CIPHER_FORMAT.equals(String.valueOf(envelope.get("format")))
                || envelope.get("ciphertext") == null) {
            return storedJson;
        }
        return secretCryptoService.decryptIfCipher(String.valueOf(envelope.get("ciphertext")));
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Flowable payload 反序列化失败", ex);
        }
    }

    private long requiredPositiveLong(String value, String field) {
        try {
            long parsed = Long.parseLong(value == null ? "" : value.trim());
            if (parsed > 0) return parsed;
        } catch (NumberFormatException ignored) {
            // 统一在下方抛出包含字段名称的异常。
        }
        throw new IllegalStateException("Flowable payload 缺少有效的 " + field);
    }

    private long number(Object value) {
        if (value instanceof Number) {
            long parsed = ((Number) value).longValue();
            if (parsed > 0) return parsed;
        }
        return requiredPositiveLong(value == null ? null : String.valueOf(value), "runId");
    }

    private String databaseText(Object value) {
        if (value == null) throw new IllegalStateException("Flowable payload 内容为空");
        if (value instanceof byte[]) return new String((byte[]) value, StandardCharsets.UTF_8);
        if (value instanceof Clob) {
            try {
                Clob clob = (Clob) value;
                return clob.getSubString(1, Math.toIntExact(clob.length()));
            } catch (Exception ex) {
                throw new IllegalStateException("Flowable payload 内容读取失败", ex);
            }
        }
        return String.valueOf(value);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item & 0xff));
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Flowable payload 摘要计算失败", ex);
        }
    }

    /**
     * 将 payload 序列化为 JSON。
     *
     * @param payload payload 数据
     * @return JSON 字符串
     */
    private String toJson(Object payload) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 序列化 payload。
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            // 抛出 payload 序列化异常。
            throw new IllegalArgumentException("Flowable payload 序列化失败", ex);
        }
    }
}
