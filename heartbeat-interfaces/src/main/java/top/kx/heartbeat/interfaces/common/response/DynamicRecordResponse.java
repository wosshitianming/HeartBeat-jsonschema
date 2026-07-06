package top.kx.heartbeat.interfaces.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.application.common.response.RecordResponse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态记录统一响应对象。
 *
 * <p>用于承接当前仍由业务服务动态组装的记录数据，避免接口层直接暴露 {@code Map<String, Object>} 泛型。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynamicRecordResponse {

    /**
     * 记录字段集合。
     */
    private Map<String, Object> fields;

    /**
     * 将动态记录转换为响应对象。
     *
     * @param record 动态记录字段
     * @return 统一动态记录响应
     */
    public static DynamicRecordResponse from(Map<String, Object> record) {
        // 保护空记录，避免接口返回 null 字段集合。
        Map<String, Object> safeRecord = record == null ? Collections.emptyMap() : record;
        // 复制字段集合，避免外部继续修改响应对象内部状态。
        Map<String, Object> copiedRecord = new LinkedHashMap<>(safeRecord);
        // 返回统一响应对象。
        return new DynamicRecordResponse(copiedRecord);
    }

    /**
     * 将应用层通用记录响应转换为接口响应对象。
     *
     * @param record 应用层通用记录响应
     * @return 统一动态记录响应
     */
    public static DynamicRecordResponse from(RecordResponse record) {
        return record == null ? from(Collections.emptyMap()) : from(record.toMap());
    }

    /**
     * 将动态记录列表转换为响应对象列表。
     *
     * @param records 动态记录列表
     * @return 统一动态记录响应列表
     */
    public static List<DynamicRecordResponse> fromList(List<Map<String, Object>> records) {
        // 保护空列表，避免流式转换出现空指针。
        List<Map<String, Object>> safeRecords = records == null ? Collections.emptyList() : records;
        // 逐条转换为响应对象。
        return safeRecords.stream().map(DynamicRecordResponse::from).collect(Collectors.toList());
    }

    /**
     * 将应用层通用记录响应列表转换为接口响应列表。
     *
     * @param records 应用层通用记录响应列表
     * @return 统一动态记录响应列表
     */
    public static List<DynamicRecordResponse> fromRecordList(List<RecordResponse> records) {
        List<RecordResponse> safeRecords = records == null ? Collections.emptyList() : records;
        return safeRecords.stream().map(DynamicRecordResponse::from).collect(Collectors.toList());
    }
}
