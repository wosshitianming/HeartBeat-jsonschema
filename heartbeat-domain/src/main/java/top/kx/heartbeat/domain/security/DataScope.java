package top.kx.heartbeat.domain.security;

import java.util.List;

/**
 * 数据权限范围。
 */
public enum DataScope {

    /**
     * 全部数据。
     */
    ALL("ALL"),

    /**
     * 本部门及子部门数据。
     */
    DEPT_AND_CHILD("DEPT_AND_CHILD"),

    /**
     * 本部门数据。
     */
    DEPT("DEPT"),

    /**
     * 自定义部门数据。
     */
    CUSTOM("CUSTOM"),

    /**
     * 仅本人数据。
     */
    SELF("SELF");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    DataScope(String code) {
        this.code = code;
    }

    /**
     * 按优先级从角色范围中解析有效范围。
     */
    public static DataScope resolve(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return SELF;
        }
        if (scopes.contains(ALL.code)) {
            return ALL;
        }
        if (scopes.contains(DEPT_AND_CHILD.code)) {
            return DEPT_AND_CHILD;
        }
        if (scopes.contains(DEPT.code)) {
            return DEPT;
        }
        if (scopes.contains(CUSTOM.code)) {
            return CUSTOM;
        }
        return SELF;
    }

    /**
     * 判断编码是否等于当前范围。
     */
    public boolean matches(String value) {
        return code.equals(String.valueOf(value));
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
