package top.kx.heartbeat.application.structure.dto;

import lombok.Value;

/**
 * 结构数据校验错误数据传输对象。
 *
 * <p>用于描述结构校验中的单个错误。</p>
 */
@Value
public class ValidationError {
    /**
     * 错误所在路径。
     */
    String path;
    /**
     * 触发错误的校验关键字。
     */
    String keyword;
    /**
     * 期望值描述。
     */
    String expected;
    /**
     * 实际值描述。
     */
    String actual;
    /**
     * 错误提示文案。
     */
    String message;
}
