package top.kx.heartbeat.application.structure.dto;

/**
 * 结构校验模式枚举。
 *
 * <p>用于控制结构校验时的宽松或严格策略。</p>
 */
public enum ValidationMode {

    /**
     * 宽松校验模式。
     */
    LENIENT,

    /**
     * 严格校验模式。
     */
    STRICT
}
