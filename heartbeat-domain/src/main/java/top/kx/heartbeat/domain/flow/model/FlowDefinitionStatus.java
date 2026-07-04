package top.kx.heartbeat.domain.flow.model;

/**
 * 流程定义状态枚举。
 *
 * <p>用于统一流程定义草稿、保存等状态编码。</p>
 */
public enum FlowDefinitionStatus {

    /**
     * 草稿状态。
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 已保存状态。
     */
    SAVED("SAVED", "已保存");

    /**
     * 状态编码。
     */
    private final String code;

    /**
     * 状态描述。
     */
    private final String description;

    /**
     * 创建流程定义状态枚举。
     *
     * @param code 状态编码
     * @param description 状态描述
     */
    FlowDefinitionStatus(String code, String description) {
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
