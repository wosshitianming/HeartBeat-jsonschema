package top.kx.heartbeat.domain.user.model.valueobject;

/**
 * 用户状态。
 *
 * <p>用枚举显式表达聚合的生命周期状态，状态流转规则由聚合根 {@code User} 负责约束。
 */
public enum UserStatus {

    /**
     * 已激活，可正常使用。
     */
    ACTIVE,

    /**
     * 已停用，禁止登录与操作。
     */
    DISABLED
}
