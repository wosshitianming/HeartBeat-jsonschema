package top.kx.heartbeat.domain.flow.model;

/**
 * 节点组件状态枚举。
 *
 * <p>用于统一节点组件的持久化状态编码。</p>
 */
public enum NodeComponentStatus {

    /**
     * 已启用状态。
     */
    ACTIVE("ACTIVE");

    /**
     * 状态编码。
     */
    private final String code;

    /**
     * 创建节点组件状态枚举。
     *
     * @param code 状态编码
     */
    NodeComponentStatus(String code) {
        // 绑定状态编码。
        this.code = code;
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
     * 判断编码是否等于当前状态。
     *
     * @param value 待判断状态编码
     * @return 是否匹配当前状态
     */
    public boolean matches(String value) {
        // 忽略大小写匹配状态编码。
        return code.equalsIgnoreCase(String.valueOf(value));
    }
}
