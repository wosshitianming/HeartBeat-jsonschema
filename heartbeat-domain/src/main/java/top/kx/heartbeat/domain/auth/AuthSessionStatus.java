package top.kx.heartbeat.domain.auth;

/**
 * 认证会话状态。
 */
public enum AuthSessionStatus {

    /**
     * 活跃会话。
     */
    ACTIVE("ACTIVE"),

    /**
     * 已撤销会话。
     */
    REVOKED("REVOKED");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    AuthSessionStatus(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
