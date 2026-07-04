package top.kx.heartbeat.domain.event;

/**
 * 可靠事件状态。
 */
public enum ReliableEventStatus {

    /**
     * 新建待投递。
     */
    NEW("NEW"),

    /**
     * 已消费处理。
     */
    PROCESSED("PROCESSED");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    ReliableEventStatus(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
