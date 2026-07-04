package top.kx.heartbeat.infrastructure.tenant;

import org.springframework.stereotype.Component;

/**
 * HeartBeat 租户工厂（兼容层）。
 * <p>
 * Tenant isolation is handled by the MyBatis interceptor and TenantContext.
 * 本类保留为 Spring Bean，方便业务方按需注入。
 * </p>
 *
 * @author heartbeat-team
 */
@Component
public class HeartbeatTenantFactory {

    /**
     * 获取当前线程允许访问的租户标识。
     *
     * @return 租户标识数组
     */
    public Object[] getTenantIds() {
        if (TenantContext.isPlatformScope()) {
            return new Object[0];
        }
        return new Object[]{TenantContext.getRequiredTenantId()};
    }
}