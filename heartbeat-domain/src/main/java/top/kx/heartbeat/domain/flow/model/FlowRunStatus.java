package top.kx.heartbeat.domain.flow.model;

/**
 * 流程运行状态枚举。
 *
 * <p>用于统一流程运行与节点执行成功失败状态编码。</p>
 */
public enum FlowRunStatus {

    /**
     * 已创建状态。
     */
    CREATED("CREATED", "已创建"),

    /**
     * 运行中状态。
     */
    RUNNING("RUNNING", "运行中"),

    /**
     * 等待中状态。
     */
    WAITING("WAITING", "等待中"),

    /**
     * 成功状态。
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 失败状态。
     */
    FAILED("FAILED", "失败"),

    /**
     * 已取消状态。
     */
    CANCELED("CANCELED", "已取消"),

    /**
     * 已超时状态。
     */
    TIMEOUT("TIMEOUT", "已超时");

    /**
     * 状态编码。
     */
    private final String code;

    /**
     * 状态描述。
     */
    private final String description;

    /**
     * 创建流程运行状态枚举。
     *
     * @param code 状态编码
     * @param description 状态描述
     */
    FlowRunStatus(String code, String description) {
        // 绑定状态编码。
        this.code = code;
        // 绑定状态描述。
        this.description = description;
    }

    /**
     * 获取状态编码。
     *
     * @return 状态编码
     */
    public String getCode() {
        // 返回状态编码。
        return code;
    }

    /**
     * 获取状态描述。
     *
     * @return 状态描述
     */
    public String getDescription() {
        // 返回状态描述。
        return description;
    }

    /**
     * 判断状态编码是否匹配当前枚举。
     *
     * @param code 状态编码
     * @return 是否匹配当前状态
     */
    public boolean matches(String code) {
        // 比较外部编码与当前枚举编码。
        return this.code.equals(code);
    }
}
