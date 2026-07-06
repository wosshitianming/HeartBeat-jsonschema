package top.kx.heartbeat.application.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用记录响应。
 *
 * <p>用于承接尚未拆成强类型字段的历史动态资源，避免应用服务直接返回 {@code Map<String, Object>}。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordResponse {

    /**
     * 响应字段。
     */
    private Map<String, Object> fields;

    public static RecordResponse from(Map<String, Object> fields) {
        Map<String, Object> safeFields = fields == null ? Collections.emptyMap() : fields;
        return new RecordResponse(new LinkedHashMap<>(safeFields));
    }

    public static RecordResponse from(DomainRecord record) {
        return record == null ? from(Collections.emptyMap()) : from(record.toMap());
    }

    public static List<RecordResponse> fromMaps(List<Map<String, Object>> records) {
        List<Map<String, Object>> safeRecords = records == null ? Collections.emptyList() : records;
        return safeRecords.stream().map(RecordResponse::from).collect(Collectors.toList());
    }

    public static List<RecordResponse> fromRecords(List<DomainRecord> records) {
        List<DomainRecord> safeRecords = records == null ? Collections.emptyList() : records;
        return safeRecords.stream().map(RecordResponse::from).collect(Collectors.toList());
    }

    public Object get(String key) {
        return safeFields().get(key);
    }

    public void put(String key, Object value) {
        safeFields().put(key, value);
    }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(safeFields());
    }

    private Map<String, Object> safeFields() {
        if (fields == null) {
            fields = new LinkedHashMap<>();
        }
        return fields;
    }
}
