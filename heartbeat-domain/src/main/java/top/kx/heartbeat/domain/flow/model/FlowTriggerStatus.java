package top.kx.heartbeat.domain.flow.model;

/**
 * 流程触发器状态枚举。
 *
 * <p>用于表达触发器草稿、启用、禁用和因中间件开关被禁用等状态。</p>
 */
public enum FlowTriggerStatus {

    /**
     * 草稿状态。
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 启用状态。
     */
    ACTIVE("ACTIVE", "启用"),

    /**
     * 人工禁用状态。
     */
    DISABLED("DISABLED", "禁用"),

    /**
     * 中间件未启用导致禁用状态。
     */
    DISABLED_BY_MIDDLEWARE("DISABLED_BY_MIDDLEWARE", "中间件未启用"),

    /**
     * 错误状态。
     */
    ERROR("ERROR", "错误");

    /**
     * 状态编码。
     */
    private final String code;

    /**
     * 状态描述。
     */
    private final String description;

    /**
     * 创建流程触发器状态枚举。
     *
     * @param code 状态编码
     * @param description 状态描述
     */
    FlowTriggerStatus(String code, String description) {
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
}
