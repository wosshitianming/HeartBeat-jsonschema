package top.kx.heartbeat.domain.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台资源与权限前缀映射。
 */
public enum PermissionResource {

    /**
     * 租户资源。
     */
    TENANTS("tenants", "system:tenant"),

    /**
     * 用户资源。
     */
    USERS("users", "system:user"),

    /**
     * 部门资源。
     */
    DEPTS("depts", "system:dept"),

    /**
     * 岗位资源。
     */
    POSTS("posts", "system:post"),

    /**
     * 角色资源。
     */
    ROLES("roles", "system:role"),

    /**
     * 字典类型资源。
     */
    DICT_TYPES("dict-types", "system:dict"),

    /**
     * 字典数据资源。
     */
    DICT_DATA("dict-data", "system:dict"),

    /**
     * 参数配置资源。
     */
    CONFIGS("configs", "system:config"),

    /**
     * 通知公告资源。
     */
    NOTICES("notices", "system:notice"),

    /**
     * 操作日志资源。
     */
    OPER_LOGS("oper-logs", "monitor:operlog"),

    /**
     * 登录日志资源。
     */
    LOGIN_LOGS("login-logs", "monitor:loginlog"),

    /**
     * 在线会话资源。
     */
    ONLINE_SESSIONS("online-sessions", "monitor:online"),

    /**
     * 定时任务资源。
     */
    JOBS("jobs", "tool:job"),

    /**
     * 定时任务日志资源。
     */
    JOB_LOGS("job-logs", "tool:job"),

    /**
     * OAuth 客户端资源。
     */
    OAUTH_CLIENTS("oauth-clients", "system:oauth"),

    /**
     * 社交登录渠道资源。
     */
    SOCIAL_PROVIDERS("social-providers", "system:social"),

    /**
     * 代码生成表资源。
     */
    GEN_TABLES("gen-tables", "tool:gen"),

    /**
     * 代码生成字段资源。
     */
    GEN_COLUMNS("gen-columns", "tool:gen"),

    /**
     * 工作流定义资源。
     */
    WORKFLOW_DEFINITIONS("workflow-definitions", "biz:workflow"),

    /**
     * 工作流实例资源。
     */
    WORKFLOW_INSTANCES("workflow-instances", "biz:workflow"),

    /**
     * 工作流任务资源。
     */
    WORKFLOW_TASKS("workflow-tasks", "biz:workflow"),

    /**
     * 支付渠道资源。
     */
    PAY_CHANNELS("pay-channels", "biz:pay"),

    /**
     * 支付订单资源。
     */
    PAY_ORDERS("pay-orders", "biz:pay"),

    /**
     * 支付通知资源。
     */
    PAY_NOTIFIES("pay-notifies", "biz:pay"),

    /**
     * 公众号账号资源。
     */
    MP_ACCOUNTS("mp-accounts", "biz:mp"),

    /**
     * 公众号菜单资源。
     */
    MP_MENUS("mp-menus", "biz:mp"),

    /**
     * 公众号素材资源。
     */
    MP_MATERIALS("mp-materials", "biz:mp"),

    /**
     * 公众号自动回复资源。
     */
    MP_AUTO_REPLIES("mp-auto-replies", "biz:mp"),

    /**
     * 报表数据集资源。
     */
    REPORT_DATASETS("report-datasets", "biz:report"),

    /**
     * 报表模板资源。
     */
    REPORT_TEMPLATES("report-templates", "biz:report"),

    /**
     * 报表查询日志资源。
     */
    REPORT_QUERY_LOGS("report-query-logs", "biz:report"),

    /**
     * 移动应用资源。
     */
    MOBILE_APPS("mobile-apps", "biz:mobile"),

    /**
     * 移动页面资源。
     */
    MOBILE_PAGES("mobile-pages", "biz:mobile"),

    /**
     * 移动接口路由资源。
     */
    MOBILE_API_ROUTES("mobile-api-routes", "biz:mobile");

    /**
     * 资源标识。
     */
    private final String resource;

    /**
     * 权限前缀。
     */
    private final String permissionPrefix;

    /**
     * 资源索引。
     */
    private static final Map<String, PermissionResource> INDEX = buildIndex();

    /**
     * 绑定资源标识与权限前缀。
     */
    PermissionResource(String resource, String permissionPrefix) {
        this.resource = resource;
        this.permissionPrefix = permissionPrefix;
    }

    /**
     * 按资源标识查找权限资源。
     */
    public static PermissionResource fromResource(String resource) {
        return INDEX.get(resource);
    }

    /**
     * 生成完整权限编码。
     */
    public String permissionOf(PermissionAction action) {
        return permissionPrefix + ":" + action.getCode();
    }

    /**
     * 构建只读索引。
     */
    private static Map<String, PermissionResource> buildIndex() {
        Map<String, PermissionResource> index = new LinkedHashMap<>();
        for (PermissionResource permissionResource : values()) {
            index.put(permissionResource.resource, permissionResource);
        }
        return Collections.unmodifiableMap(index);
    }
}
