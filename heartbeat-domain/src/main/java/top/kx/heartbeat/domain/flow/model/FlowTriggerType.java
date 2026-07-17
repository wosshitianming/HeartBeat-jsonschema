package top.kx.heartbeat.domain.flow.model;

/**
 * 流程触发类型枚举。
 *
 * <p>用于统一手动、Webhook、定时、消息和领域事件等生产态触发入口。</p>
 */
public enum FlowTriggerType {

    /**
     * 手动触发。
     */
    MANUAL("MANUAL", "手动触发"),

    /**
     * Webhook 触发。
     */
    WEBHOOK("WEBHOOK", "Webhook 触发"),

    /**
     * Cron 定时触发。
     */
    CRON("CRON", "Cron 定时触发"),

    /**
     * MQ 消息触发。
     */
    MQ("MQ", "MQ 消息触发"),

    /**
     * 领域事件触发。
     */
    DOMAIN_EVENT("DOMAIN_EVENT", "领域事件触发"),

    /**
     * 本地调试触发。
     */
    DEBUG("DEBUG", "本地调试触发"),

    RETRY("RETRY", "失败执行重试");

    /**
     * 触发类型编码。
     */
    private final String code;

    /**
     * 触发类型描述。
     */
    private final String description;

    /**
     * 创建流程触发类型枚举。
     *
     * @param code 触发类型编码
     * @param description 触发类型描述
     */
    FlowTriggerType(String code, String description) {
        // 绑定触发类型编码。
        this.code = code;
        // 绑定触发类型描述。
        this.description = description;
    }

    /**
     * 获取触发类型编码。
     *
     * @return 触发类型编码
     */
    public String getCode() {
        // 返回触发类型编码。
        return code;
    }

    /**
     * 获取触发类型描述。
     *
     * @return 触发类型描述
     */
    public String getDescription() {
        // 返回触发类型描述。
        return description;
    }
}
