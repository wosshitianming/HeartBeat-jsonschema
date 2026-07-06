package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方账号绑定关系持久化对象（对应表 auth_social_binding）
 * <p>
 * 记录本地用户与第三方渠道账号之间的绑定关系及最近登录情况。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class AuthSocialBindingEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 渠道 ID（关联 AuthSocialProviderEntity） */
    private Long providerId;

    /** 本地用户 ID */
    private Long userId;

    /** 第三方用户 ID */
    private String externalUserId;

    /** 第三方 UnionID（跨应用统一标识） */
    private String externalUnionId;

    /** 第三方用户名 */
    private String externalUsername;

    /** 第三方头像 */
    private String externalAvatar;

    /** 绑定状态（ACTIVE/REVOKED） */
    private String bindingStatus;

    /** 首次绑定时间 */
    private LocalDateTime boundAt;

    /** 最近一次使用该绑定登录时间 */
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者 */
    private String createBy;

    /** 更新者 */
    private String updateBy;
}
