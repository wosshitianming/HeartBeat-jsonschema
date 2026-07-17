package top.kx.heartbeat.domain.flow.model;

/**
 * 节点组件来源枚举。
 *
 * <p>用于统一节点组件来源的持久化编码。</p>
 */
public enum NodeComponentSource {

    /**
     * 数据库注册来源。
     */
    DATABASE("DATABASE", "数据库注册"),

    /**
     * Spring 托管的 Java 代码节点。
     */
    CODE("CODE", "Java 代码"),

    /**
     * 系统内置来源。
     */
    BUILTIN("BUILTIN", "系统内置");

    /**
     * 来源编码。
     */
    private final String code;

    /**
     * 来源描述。
     */
    private final String description;

    /**
     * 创建节点组件来源枚举。
     *
     * @param code 来源编码
     * @param description 来源描述
     */
    NodeComponentSource(String code, String description) {
        // 绑定来源编码。
        this.code = code;
        // 绑定来源描述。
        this.description = description;
    }

    /**
     * 获取来源编码。
     *
     * @return 来源编码
     */
    public String getCode() {
        // 返回来源编码。
        return code;
    }

    /**
     * 获取来源描述。
     *
     * @return 来源描述
     */
    public String getDescription() {
        // 返回来源描述。
        return description;
    }
}
