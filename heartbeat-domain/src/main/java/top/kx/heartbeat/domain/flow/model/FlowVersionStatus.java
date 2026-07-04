package top.kx.heartbeat.domain.flow.model;

/**
 * 流程版本状态枚举。
 *
 * <p>用于统一流程版本发布状态编码。</p>
 */
public enum FlowVersionStatus {

    /**
     * 已发布状态。
     */
    PUBLISHED("PUBLISHED", "已发布");

    /**
     * 状态编码。
     */
    private final String code;

    /**
     * 状态描述。
     */
    private final String description;

    /**
     * 创建流程版本状态枚举。
     *
     * @param code 状态编码
     * @param description 状态描述
     */
    FlowVersionStatus(String code, String description) {
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
