package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * OAuth 客户端持久化对象（对应表 auth_oauth_client）
 * <p>
 * 描述一个第三方接入的 OAuth2 客户端及其令牌生命周期。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class AuthOauthClientEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 客户端 ID（对外暴露） */
    private String clientId;

    /** 客户端名称 */
    private String clientName;

    /** 客户端密钥散列值 */
    private String clientSecretHash;

    /** 访问令牌 TTL（秒） */
    private Integer accessTokenTtl;

    /** 刷新令牌 TTL（秒） */
    private Integer refreshTokenTtl;

    /** 状态 */
    private String status;

    /** 乐观锁版本号 */
    private Integer version;
}
