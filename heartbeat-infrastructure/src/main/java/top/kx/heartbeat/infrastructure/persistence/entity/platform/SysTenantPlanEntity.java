package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户套餐持久化对象（对应表 sys_tenant_plan）
 * <p>
 * 套餐规定可分配给租户的资源上限与功能集合。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysTenantPlanEntity {

    /** 主键 ID */
    private Long id;

    /** 套餐编码（全局唯一） */
    private String planCode;

    /** 套餐名称 */
    private String planName;

    /** 套餐类型（FREE/TRIAL/PAID/...） */
    private String planType;

    /** 描述 */
    private String description;

    /** 最大用户数 */
    private Integer maxUserCount;

    /** 最大存储（MB） */
    private Long maxStorageMb;

    /** 功能策略 JSON */
    private String featurePolicy;

    /** 排序号 */
    private Integer sortNo;

    /** 状态 */
    private String status;

    /** 乐观锁版本号 */
    private Integer version;

    /** 逻辑删除标记 */
    private Long deleteMarker;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者 */
    private String createBy;

    /** 更新者 */
    private String updateBy;
}
