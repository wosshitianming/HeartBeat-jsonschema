package top.kx.heartbeat.domain.platform;

/**
 * 平台权限类型。
 */
public enum PlatformPermissionType {

    /**
     * HTTP API 权限。
     */
    API("API");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PlatformPermissionType(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
