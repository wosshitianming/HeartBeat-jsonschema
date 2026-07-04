package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * 套餐功能配置持久化对象（对应表 sys_plan_feature）
 * <p>
 * 描述某个套餐默认开启了哪些功能。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysPlanFeatureEntity {

    /** 主键 ID */
    private Long id;

    /** 所属套餐 ID */
    private Long planId;

    /** 功能编码 */
    private String featureCode;

    /** 功能名称 */
    private String featureName;

    /** 功能类型 */
    private String featureType;

    /** 是否默认启用 */
    private Boolean enabled;

    /** 乐观锁版本号 */
    private Integer version;
}
