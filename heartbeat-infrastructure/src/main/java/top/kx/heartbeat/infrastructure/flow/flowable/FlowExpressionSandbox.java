package top.kx.heartbeat.infrastructure.flow.flowable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Flow 表达式沙盒。
 *
 * <p>用于在受控范围内执行简单条件表达式，避免用户 DSL 直接进入 Flowable UEL。</p>
 */
@Component
public class FlowExpressionSandbox {

    /**
     * 执行布尔表达式。
     *
     * @param expression 表达式
     * @param payload 输入载荷
     * @return 是否命中表达式
     */
    public boolean evaluate(String expression, Map<String, Object> payload) {
        // 标准化表达式。
        String expr = StringUtils.trimToEmpty(expression);
        // 判断是否为空表达式。
        if (StringUtils.isBlank(expr)) {
            // 空表达式不命中。
            return false;
        }
        // 判断是否为大于表达式。
        if (expr.contains(">")) {
            // 执行大于表达式。
            return evaluateGreaterThan(expr, payload);
        }
        // 判断是否为等值表达式。
        if (expr.contains("==")) {
            // 执行等值表达式。
            return evaluateEquals(expr, payload);
        }
        // 未支持表达式默认不命中。
        return false;
    }

    /**
     * 执行大于表达式。
     *
     * @param expression 表达式
     * @param payload 输入载荷
     * @return 是否命中表达式
     */
    private boolean evaluateGreaterThan(String expression, Map<String, Object> payload) {
        // 拆分表达式。
        String[] parts = expression.split(">", 2);
        // 判断表达式结构是否有效。
        if (parts.length != 2) {
            // 无效表达式不命中。
            return false;
        }
        // 解析实际值。
        BigDecimal actual = toDecimal(resolve(payload, parts[0]));
        // 解析期望值。
        BigDecimal expected = toDecimal(parts[1]);
        // 判断数值是否可比较。
        if (actual == null || expected == null) {
            // 不可比较时不命中。
            return false;
        }
        // 返回比较结果。
        return actual.compareTo(expected) > 0;
    }

    /**
     * 执行等值表达式。
     *
     * @param expression 表达式
     * @param payload 输入载荷
     * @return 是否命中表达式
     */
    private boolean evaluateEquals(String expression, Map<String, Object> payload) {
        // 拆分表达式。
        String[] parts = expression.split("==", 2);
        // 判断表达式结构是否有效。
        if (parts.length != 2) {
            // 无效表达式不命中。
            return false;
        }
        // 解析实际值。
        Object actual = resolve(payload, parts[0]);
        // 解析期望值。
        String expected = trimLiteral(parts[1]);
        // 返回等值比较结果。
        return StringUtils.equals(String.valueOf(actual), expected);
    }

    /**
     * 解析 payload 字段路径。
     *
     * @param payload 输入载荷
     * @param path 字段路径
     * @return 字段值
     */
    private Object resolve(Map<String, Object> payload, String path) {
        // 标准化字段路径。
        String normalized = StringUtils.trimToEmpty(path).replace("payload.", "").replace("$.", "");
        // 判断 payload 是否为空。
        if (payload == null) {
            // 空 payload 返回空。
            return null;
        }
        // 返回字段值。
        return payload.get(normalized);
    }

    /**
     * 转换为数值。
     *
     * @param value 原始值
     * @return 数值
     */
    private BigDecimal toDecimal(Object value) {
        try {
            // 转换为 BigDecimal。
            return new BigDecimal(trimLiteral(String.valueOf(value)));
        } catch (NumberFormatException ex) {
            // 非数值返回空。
            return null;
        }
    }

    /**
     * 裁剪字面量引号。
     *
     * @param value 原始字面量
     * @return 裁剪后的字面量
     */
    private String trimLiteral(String value) {
        // 去除空白。
        String text = StringUtils.trimToEmpty(value);
        // 去除单引号。
        text = StringUtils.removeStart(StringUtils.removeEnd(text, "'"), "'");
        // 去除双引号。
        text = StringUtils.removeStart(StringUtils.removeEnd(text, "\""), "\"");
        // 返回字面量。
        return text;
    }
}
