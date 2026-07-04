package top.kx.heartbeat.domain.platform;

/**
 * 平台菜单权限模式。
 */
public enum PlatformPermissionMode {

    /**
     * 菜单与权限通过关系表绑定。
     */
    RELATION("RELATION");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PlatformPermissionMode(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
