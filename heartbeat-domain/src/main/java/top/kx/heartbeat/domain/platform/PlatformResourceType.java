package top.kx.heartbeat.domain.platform;

/**
 * 平台权限资源类型。
 */
public enum PlatformResourceType {

    /**
     * HTTP API 资源。
     */
    HTTP_API("HTTP_API");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PlatformResourceType(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
