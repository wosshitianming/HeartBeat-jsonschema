package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户持久化对象（对应表 sys_tenant）
 * <p>
 * 一个租户对应一个相对独立的数据空间，使用套餐（{@code SysTenantPlanEntity}）来约束能力。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysTenantEntity {

    /** 主键 ID */
    private Long id;

    /** 套餐 ID */
    private Long planId;

    /** 租户编码（全局唯一） */
    private String tenantCode;

    /** 租户名称 */
    private String tenantName;

    /** 租户类型（PERSONAL/TEAM/ENTERPRISE/ISV） */
    private String tenantType;

    /** 主域名 */
    private String domain;

    /** 主联系人姓名 */
    private String contactName;

    /** 主联系人电话 */
    private String contactPhone;

    /** 主联系人邮箱 */
    private String contactEmail;

    /** LOGO URL */
    private String logoUrl;

    /** 默认时区（IANA，如 Asia/Shanghai） */
    private String timezone;

    /** 默认语言（zh-CN/en-US） */
    private String locale;

    /** 到期时间（null 表示永久） */
    private LocalDateTime expireAt;

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
