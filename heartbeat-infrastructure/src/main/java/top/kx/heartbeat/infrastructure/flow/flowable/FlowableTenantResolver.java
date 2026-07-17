package top.kx.heartbeat.infrastructure.flow.flowable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.flow.model.FlowVersion;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

/**
 * Flowable 租户解析器。
 *
 * <p>用于把 HeartBeat 租户模型转换为 Flowable tenantId。</p>
 */
@Component
public class FlowableTenantResolver {

    /**
     * 默认租户标识。
     */
    @Value("${heartbeat.flow.default-tenant-id:1}")
    private String defaultTenantId;

    /**
     * 解析流程版本所属租户。
     *
     * @param version 流程版本
     * @return Flowable 租户标识
     */
    public String resolveTenantId(FlowVersion version) {
        return TenantContext.getRequiredTenantIdString();
    }

    /**
     * 解析外部传入租户标识。
     *
     * @param tenantId 外部租户标识
     * @return Flowable 租户标识
     */
    public String resolveTenantId(String tenantId) {
        // 返回外部租户标识或默认租户标识。
        return StringUtils.defaultIfBlank(tenantId, defaultTenantId);
    }
}
