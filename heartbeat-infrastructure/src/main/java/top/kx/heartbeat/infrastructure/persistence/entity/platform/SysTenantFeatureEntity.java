package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * 租户功能开关持久化对象（对应表 sys_tenant_feature）
 * <p>
 * 覆盖套餐默认配置，按租户粒度覆盖启/禁。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysTenantFeatureEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 功能编码 */
    private String featureCode;

    /** 功能名称 */
    private String featureName;

    /** 是否启用 */
    private Boolean enabled;

    /** 乐观锁版本号 */
    private Integer version;
}
