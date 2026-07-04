package top.kx.heartbeat.domain.platform;

/**
 * 平台菜单类型。
 */
public enum PlatformMenuType {

    /**
     * 目录节点。
     */
    CATALOG("CATALOG"),

    /**
     * 菜单节点。
     */
    MENU("MENU"),

    /**
     * 按钮权限节点。
     */
    BUTTON("BUTTON");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PlatformMenuType(String code) {
        this.code = code;
    }

    /**
     * 判断编码是否等于当前类型。
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
