package top.kx.heartbeat.domain.flow.model;

/**
 * 流程运行幂等范围枚举。
 *
 * <p>用于区分原始启动、用户复原重试和管理员重开等不同幂等语义。</p>
 */
public enum FlowIdempotencyScope {

    /**
     * 原始启动幂等。
     */
    START("START", "原始启动"),

    /**
     * 用户复原重试幂等。
     */
    USER_RETRY("USER_RETRY", "用户复原重试"),

    /**
     * 管理员重开幂等。
     */
    ADMIN_RESTART("ADMIN_RESTART", "管理员重开");

    /**
     * 幂等范围编码。
     */
    private final String code;

    /**
     * 幂等范围描述。
     */
    private final String description;

    /**
     * 创建流程运行幂等范围枚举。
     *
     * @param code 幂等范围编码
     * @param description 幂等范围描述
     */
    FlowIdempotencyScope(String code, String description) {
        // 绑定幂等范围编码。
        this.code = code;
        // 绑定幂等范围描述。
        this.description = description;
    }

    /**
     * 获取幂等范围编码。
     *
     * @return 幂等范围编码
     */
    public String getCode() {
        // 返回幂等范围编码。
        return code;
    }

    /**
     * 获取幂等范围描述。
     *
     * @return 幂等范围描述
     */
    public String getDescription() {
        // 返回幂等范围描述。
        return description;
    }
}
