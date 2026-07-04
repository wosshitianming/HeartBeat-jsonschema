package top.kx.heartbeat.domain.platform;

/**
 * 平台通用记录状态。
 */
public enum PlatformRecordStatus {

    /**
     * 已启用。
     */
    ENABLED("ENABLED"),

    /**
     * 已停用。
     */
    DISABLED("DISABLED");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PlatformRecordStatus(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
