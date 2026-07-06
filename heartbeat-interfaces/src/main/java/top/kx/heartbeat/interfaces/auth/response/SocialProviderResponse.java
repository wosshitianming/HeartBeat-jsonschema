package top.kx.heartbeat.interfaces.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.application.common.response.RecordResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 第三方登录渠道响应对象。
 *
 * <p>用于登录页展示可用第三方登录入口。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialProviderResponse {

    /**
     * 第三方登录渠道编码。
     */
    private String provider;

    /**
     * 第三方登录渠道名称。
     */
    private String name;

    /**
     * 第三方登录渠道图标标识。
     */
    private String icon;

    /**
     * 将动态渠道记录转换为响应对象。
     *
     * @param record 动态渠道记录
     * @return 第三方登录渠道响应对象
     */
    public static SocialProviderResponse from(Map<String, Object> record) {
        // 兜底空渠道记录。
        Map<String, Object> safeRecord = record == null ? Collections.emptyMap() : record;
        // 读取渠道编码。
        String provider = stringValue(safeRecord.get("provider"));
        // 读取渠道名称。
        String name = stringValue(safeRecord.get("name"));
        // 读取渠道图标标识。
        String icon = stringValue(safeRecord.get("icon"));
        // 返回第三方登录渠道响应对象。
        return new SocialProviderResponse(provider, name, icon);
    }

    public static SocialProviderResponse from(RecordResponse record) {
        return record == null ? from(Collections.emptyMap()) : from(record.toMap());
    }

    /**
     * 将动态渠道记录列表转换为响应对象列表。
     *
     * @param records 动态渠道记录列表
     * @return 第三方登录渠道响应对象列表
     */
    public static List<SocialProviderResponse> fromList(List<Map<String, Object>> records) {
        // 兜底空渠道记录列表。
        List<Map<String, Object>> safeRecords = records == null ? Collections.emptyList() : records;
        // 逐条转换为第三方登录渠道响应对象。
        return safeRecords.stream().map(SocialProviderResponse::from).collect(Collectors.toList());
    }

    public static List<SocialProviderResponse> fromRecordList(List<RecordResponse> records) {
        List<RecordResponse> safeRecords = records == null ? Collections.emptyList() : records;
        return safeRecords.stream().map(SocialProviderResponse::from).collect(Collectors.toList());
    }

    /**
     * 安全转换字符串。
     *
     * @param value 原始字段值
     * @return 字符串字段值
     */
    private static String stringValue(Object value) {
        // 返回非空字符串字段值。
        return value == null ? "" : String.valueOf(value);
    }
}
