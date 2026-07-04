package top.kx.heartbeat.domain.flow.model;

/**
 * 连接凭证状态枚举。
 *
 * <p>用于统一流程连接凭证的生命周期状态编码。</p>
 */
public enum ConnectionCredentialStatus {

    /**
     * 启用状态。
     */
    ACTIVE("ACTIVE", "启用");

    /**
     * 状态编码。
     */
    private final String code;

    /**
     * 状态描述。
     */
    private final String description;

    /**
     * 创建连接凭证状态枚举。
     *
     * @param code 状态编码
     * @param description 状态描述
     */
    ConnectionCredentialStatus(String code, String description) {
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
