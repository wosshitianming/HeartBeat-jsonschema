package top.kx.heartbeat.domain.event;

/**
 * 流程等待状态。
 */
public enum FlowWaitStatus {

    /**
     * 等待外部事件。
     */
    WAITING("WAITING"),

    /**
     * 已被外部事件恢复。
     */
    RESUMED("RESUMED");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    FlowWaitStatus(String code) {
        this.code = code;
    }

    /**
     * 判断编码是否等于当前状态。
     */
    public boolean matches(String value) {
        return code.equalsIgnoreCase(String.valueOf(value));
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
