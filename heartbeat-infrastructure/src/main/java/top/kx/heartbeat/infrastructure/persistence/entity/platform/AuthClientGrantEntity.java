package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * OAuth 客户端授权类型持久化对象（对应表 auth_client_grant）
 *
 * @author heartbeat-team
 */
@Data
public class AuthClientGrantEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 所属 OAuth 客户端 ID */
    private Long oauthClientId;

    /** 授权类型（如 authorization_code、password、client_credentials） */
    private String grantType;
}
