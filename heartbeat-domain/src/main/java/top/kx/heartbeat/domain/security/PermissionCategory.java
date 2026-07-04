package top.kx.heartbeat.domain.security;

/**
 * 权限所属业务分类。
 */
public enum PermissionCategory {

    /**
     * 系统管理权限前缀。
     */
    SYSTEM("system:", "系统管理"),

    /**
     * 系统监控权限前缀。
     */
    MONITOR("monitor:", "系统监控"),

    /**
     * 系统工具权限前缀。
     */
    TOOL("tool:", "系统工具"),

    /**
     * 数据配置权限前缀。
     */
    STRUCTURE("structure:", "数据配置"),

    /**
     * 业务平台权限前缀。
     */
    BIZ("biz:", "业务平台"),

    /**
     * 未命中前缀时的默认分类。
     */
    PLATFORM("", "平台菜单");

    /**
     * 权限编码前缀。
     */
    private final String prefix;

    /**
     * 页面展示名称。
     */
    private final String label;

    /**
     * 绑定权限前缀与展示名称。
     */
    PermissionCategory(String prefix, String label) {
        this.prefix = prefix;
        this.label = label;
    }

    /**
     * 按权限编码解析分类。
     */
    public static PermissionCategory resolve(String permission) {
        String normalizedPermission = permission == null ? "" : permission;
        for (PermissionCategory category : values()) {
            if (!category.prefix.isEmpty() && normalizedPermission.startsWith(category.prefix)) {
                return category;
            }
        }
        return PLATFORM;
    }

    /**
     * 返回页面展示名称。
     */
    public String getLabel() {
        return label;
    }
}
