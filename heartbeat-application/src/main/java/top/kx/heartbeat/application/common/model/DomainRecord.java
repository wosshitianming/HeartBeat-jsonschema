package top.kx.heartbeat.application.common.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 领域动态记录。
 *
 * <p>用于承接暂未建模成强类型对象的持久化行数据，并保证应用层拿到的是防御性拷贝。</p>
 */
public final class DomainRecord {

    /**
     * 记录字段集合。
     */
    private final Map<String, Object> values;

    /**
     * 构建领域动态记录。
     *
     * @param values 记录字段集合。
     */
    private DomainRecord(Map<String, Object> values) {
        // 使用不可变 Map 封装入参，避免外部修改污染领域记录。
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /**
     * 根据字段集合创建领域动态记录。
     *
     * @param values 记录字段集合。
     * @return 领域动态记录。
     */
    public static DomainRecord of(Map<String, Object> values) {
        // 空入参统一收敛为空集合，减少调用侧空指针判断。
        return new DomainRecord(values == null ? Collections.<String, Object>emptyMap() : values);
    }

    /**
     * 导出可修改字段副本。
     *
     * @return 可修改字段副本。
     */
    public Map<String, Object> toMap() {
        // 返回新的 LinkedHashMap，避免调用侧修改内部不可变集合。
        return new LinkedHashMap<>(values);
    }

    /**
     * 读取单个字段值。
     *
     * @param key 字段名。
     * @return 字段值。
     */
    public Object get(String key) {
        // 直接按字段名读取当前记录中的值。
        return values.get(key);
    }
}
