package top.kx.heartbeat.domain.flow.model;

/**
 * 流程节点运行状态枚举。
 *
 * <p>用于记录单个节点在生产态和调试态中的执行生命周期。</p>
 */
public enum FlowNodeRunStatus {

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
     * 已跳过状态。
     */
    SKIPPED("SKIPPED", "已跳过"),

    /**
     * 重试中状态。
     */
    RETRYING("RETRYING", "重试中");

    /**
     * 状态编码。
     */
    private final String code;

    /**
     * 状态描述。
     */
    private final String description;

    /**
     * 创建节点运行状态枚举。
     *
     * @param code 状态编码
     * @param description 状态描述
     */
    FlowNodeRunStatus(String code, String description) {
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
