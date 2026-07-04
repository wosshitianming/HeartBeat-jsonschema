package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * OAuth 客户端回调地址持久化对象（对应表 auth_client_redirect_uri）
 *
 * @author heartbeat-team
 */
@Data
public class AuthClientRedirectUriEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 所属 OAuth 客户端 ID */
    private Long oauthClientId;

    /** 合法回调地址 */
    private String redirectUri;
}
