package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录会话持久化对象（对应表 auth_session）
 * <p>
 * 一次登录即产生一条会话记录，跟踪访问令牌、刷新令牌、撤销与存活时间等信息。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class AuthSessionEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 会话 ID（对外不可猜） */
    private String sessionId;

    /** 登录用户 ID */
    private Long userId;

    /** 当前访问令牌的 JTI */
    private String accessTokenJti;

    /** 刷新令牌散列值 */
    private String refreshTokenHash;

    /** 设备类型（WEB/IOS/ANDROID/...） */
    private String deviceType;

    /** 设备名称 */
    private String deviceName;

    /** 登录 IP */
    private String loginIp;

    /** 浏览器 User-Agent */
    private String userAgent;

    /** 状态（ACTIVE/REVOKED/EXPIRED） */
    private String status;

    /** 颁发时间 */
    private LocalDateTime issuedAt;

    /** 访问令牌到期时间 */
    private LocalDateTime expireAt;

    /** 刷新令牌到期时间 */
    private LocalDateTime refreshExpireAt;

    /** 撤销时间（可空） */
    private LocalDateTime revokedAt;

    /** 最近访问时间 */
    private LocalDateTime lastAccessAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者 */
    private Long createBy;

    /** 更新者 */
    private Long updateBy;
}
