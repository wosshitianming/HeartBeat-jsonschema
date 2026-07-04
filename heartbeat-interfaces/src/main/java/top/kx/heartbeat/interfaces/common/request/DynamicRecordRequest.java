package top.kx.heartbeat.interfaces.common.request;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态记录通用请求对象。
 *
 * <p>用于承接尚未完全固化字段结构的请求体，避免接口层方法签名直接暴露 {@code Map<String, Object>}。</p>
 */
@Data
@NoArgsConstructor
public class DynamicRecordRequest {

    /**
     * 动态请求字段集合。
     */
    private Map<String, Object> fields = new LinkedHashMap<>();

    /**
     * 接收 JSON 中未显式建模的动态字段。
     *
     * @param name 字段名称
     * @param value 字段值
     */
    @JsonAnySetter
    public void put(String name, Object value) {
        // 将动态字段写入字段集合。
        fields.put(name, value);
    }

    /**
     * 输出动态字段集合。
     *
     * @return 动态字段集合
     */
    @JsonAnyGetter
    public Map<String, Object> any() {
        // 返回动态字段集合。
        return fields;
    }

    /**
     * 转换为业务层使用的字段映射。
     *
     * @return 安全复制后的字段映射
     */
    @JsonIgnore
    public Map<String, Object> toMap() {
        // 兜底空字段集合。
        Map<String, Object> safeFields = fields == null ? Collections.emptyMap() : fields;
        // 返回复制后的字段映射。
        return new LinkedHashMap<>(safeFields);
    }
}
