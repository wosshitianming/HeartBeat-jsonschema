package top.kx.heartbeat.domain.security;

/**
 * 权限动作枚举。
 */
public enum PermissionAction {

    /**
     * 查询动作。
     */
    LIST("list"),

    /**
     * 新增动作。
     */
    ADD("add"),

    /**
     * 修改动作。
     */
    EDIT("edit"),

    /**
     * 删除动作。
     */
    REMOVE("remove");

    /**
     * 权限编码后缀。
     */
    private final String code;

    /**
     * 绑定权限编码后缀。
     */
    PermissionAction(String code) {
        this.code = code;
    }

    /**
     * 将接口资源动作转换为权限动作。
     */
    public static PermissionAction fromResourceAction(String action) {
        if ("create".equals(action)) {
            return ADD;
        }
        if ("update".equals(action)) {
            return EDIT;
        }
        if ("delete".equals(action)) {
            return REMOVE;
        }
        return LIST;
    }

    /**
     * 返回权限编码后缀。
     */
    public String getCode() {
        return code;
    }
}
