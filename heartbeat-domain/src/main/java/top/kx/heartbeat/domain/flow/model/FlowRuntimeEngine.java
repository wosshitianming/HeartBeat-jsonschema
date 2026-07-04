package top.kx.heartbeat.domain.flow.model;

/**
 * 流程运行时引擎枚举。
 *
 * <p>用于区分本地调试执行器与生产态 Flowable 引擎。</p>
 */
public enum FlowRuntimeEngine {

    /**
     * 本地调试执行器。
     */
    LOCAL_DEBUG("LOCAL_DEBUG", "本地调试"),

    /**
     * Flowable 生产态引擎。
     */
    FLOWABLE("FLOWABLE", "Flowable");

    /**
     * 引擎编码。
     */
    private final String code;

    /**
     * 引擎描述。
     */
    private final String description;

    /**
     * 创建流程运行时引擎枚举。
     *
     * @param code 引擎编码
     * @param description 引擎描述
     */
    FlowRuntimeEngine(String code, String description) {
        // 绑定引擎编码。
        this.code = code;
        // 绑定引擎描述。
        this.description = description;
    }

    /**
     * 获取引擎编码。
     *
     * @return 引擎编码
     */
    public String getCode() {
        // 返回引擎编码。
        return code;
    }

    /**
     * 获取引擎描述。
     *
     * @return 引擎描述
     */
    public String getDescription() {
        // 返回引擎描述。
        return description;
    }

    /**
     * 按编码解析运行时引擎。
     *
     * @param code 引擎编码
     * @return 运行时引擎枚举
     */
    public static FlowRuntimeEngine fromCode(String code) {
        // 遍历全部运行时引擎。
        for (FlowRuntimeEngine engine : values()) {
            // 判断当前引擎编码是否命中。
            if (engine.code.equalsIgnoreCase(code)) {
                // 返回命中的运行时引擎。
                return engine;
            }
        }
        // 返回默认生产态 Flowable 引擎。
        return FLOWABLE;
    }
}
