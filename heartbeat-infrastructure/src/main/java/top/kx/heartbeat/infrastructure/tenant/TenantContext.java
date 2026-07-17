package top.kx.heartbeat.infrastructure.tenant;


import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

/**
 * 租户上下文。
 *
 * <p>基于 ThreadLocal 保存当前请求租户与平台级访问标识，调用结束必须清理。</p>
 */
public final class TenantContext {

    /**
     * 当前线程租户标识持有器。
     */
    private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();

    /**
     * 当前线程平台级作用域持有器。
     */
    private static final ThreadLocal<Boolean> PLATFORM_SCOPE_HOLDER = new ThreadLocal<>();

    /**
     * 禁止实例化工具类。
     */
    private TenantContext() {
    }

    /**
     * 设置当前线程租户标识。
     *
     * @param tenantId 租户标识。
     */
    public static void setTenantId(long tenantId) {
        // 租户标识必须为正数。
        if (tenantId <= 0) {
            // 抛出租户标识非法异常。
            throw new IllegalArgumentException("tenant id must be positive");
        }
        // 写入当前线程租户标识。
        TENANT_HOLDER.set(tenantId);
        // 设置租户标识后清除平台级作用域。
        PLATFORM_SCOPE_HOLDER.remove();
    }

    /**
     * 设置当前线程租户标识。
     *
     * @param tenantId 租户标识字符串。
     */
    public static void setTenantId(String tenantId) {
        // 租户标识字符串不能为空。
        if (StringUtils.isBlank(tenantId)) {
            // 抛出租户标识为空异常。
            throw new IllegalArgumentException("tenant id must not be blank");
        }
        // 捕获数字解析异常并转换为租户上下文异常。
        try {
            // 将字符串租户标识解析为长整型并写入上下文。
            setTenantId(Long.parseLong(tenantId.trim()));
        } catch (NumberFormatException ex) {
            // 抛出租户标识类型非法异常。
            throw new IllegalArgumentException("tenant id must be a long value", ex);
        }
    }

    /**
     * 获取当前线程租户标识。
     *
     * @return 当前线程租户标识。
     */
    public static Long getTenantId() {
        // 平台级作用域不返回具体租户标识。
        if (isPlatformScope()) {
            // 返回空租户标识。
            return null;
        }
        // 返回当前线程租户标识。
        return TENANT_HOLDER.get();
    }

    /**
     * 获取当前线程必填租户标识。
     *
     * @return 当前线程租户标识。
     */
    public static long getRequiredTenantId() {
        // 获取当前线程租户标识。
        Long tenantId = getTenantId();
        // 租户标识不存在时说明请求未完成认证上下文绑定。
        if (tenantId == null) {
            // 抛出租户上下文缺失异常。
            throw new IllegalStateException("tenant context is not authenticated");
        }
        // 返回当前线程租户标识。
        return tenantId;
    }

    /**
     * 获取当前线程必填租户标识字符串。
     *
     * @return 当前线程租户标识字符串。
     */
    public static String getRequiredTenantIdString() {
        // 将必填租户标识转换为字符串。
        return String.valueOf(getRequiredTenantId());
    }

    /**
     * 在指定租户作用域内执行动作，并完整恢复原租户或平台级上下文。
     */
    public static <T> T runAsTenant(String tenantId, Supplier<T> action) {
        if (StringUtils.isBlank(tenantId)) {
            throw new IllegalArgumentException("tenant id must not be blank");
        }
        try {
            return runAsTenant(Long.parseLong(tenantId.trim()), action);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("tenant id must be a long value", ex);
        }
    }

    /**
     * 在指定租户作用域内执行动作，并完整恢复原租户或平台级上下文。
     */
    public static <T> T runAsTenant(long tenantId, Supplier<T> action) {
        Long previousTenantId = TENANT_HOLDER.get();
        Boolean previousPlatformScope = PLATFORM_SCOPE_HOLDER.get();
        try {
            setTenantId(tenantId);
            return action.get();
        } finally {
            if (previousTenantId == null) TENANT_HOLDER.remove();
            else TENANT_HOLDER.set(previousTenantId);
            if (previousPlatformScope == null) PLATFORM_SCOPE_HOLDER.remove();
            else PLATFORM_SCOPE_HOLDER.set(previousPlatformScope);
        }
    }

    /**
     * 以平台级作用域执行动作。
     *
     * @param action 平台级动作。
     * @param <T> 返回值类型。
     * @return 动作执行结果。
     */
    public static <T> T runAsPlatform(Supplier<T> action) {
        // 记录进入平台级作用域前的租户标识。
        Long previousTenantId = TENANT_HOLDER.get();
        // 记录进入平台级作用域前的平台标识。
        Boolean previousPlatformScope = PLATFORM_SCOPE_HOLDER.get();
        // 临时切换为平台级作用域。
        try {
            // 清除当前线程租户标识。
            TENANT_HOLDER.remove();
            // 标记当前线程为平台级作用域。
            PLATFORM_SCOPE_HOLDER.set(Boolean.TRUE);
            // 执行平台级动作。
            return action.get();
        } finally {
            // 恢复进入前的租户标识。
            if (previousTenantId == null) {
                // 原租户为空时移除租户上下文。
                TENANT_HOLDER.remove();
            } else {
                // 原租户存在时恢复租户上下文。
                TENANT_HOLDER.set(previousTenantId);
            }
            // 恢复进入前的平台级作用域标识。
            if (previousPlatformScope == null) {
                // 原平台标识为空时移除平台上下文。
                PLATFORM_SCOPE_HOLDER.remove();
            } else {
                // 原平台标识存在时恢复平台上下文。
                PLATFORM_SCOPE_HOLDER.set(previousPlatformScope);
            }
        }
    }

    /**
     * 判断当前线程是否为平台级作用域。
     *
     * @return 是否为平台级作用域。
     */
    static boolean isPlatformScope() {
        // 判断平台级作用域标识是否为真。
        return Boolean.TRUE.equals(PLATFORM_SCOPE_HOLDER.get());
    }

    /**
     * 清理当前线程租户上下文。
     */
    public static void clear() {
        // 清理当前线程租户标识。
        TENANT_HOLDER.remove();
        // 清理当前线程平台级作用域标识。
        PLATFORM_SCOPE_HOLDER.remove();
    }
}
