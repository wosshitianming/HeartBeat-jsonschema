package top.kx.heartbeat.application.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.domain.auth.AuthTokenPayload;

/**
 * 认证令牌响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String tenantId;
    private String sessionId;

    public static AuthTokenResponse from(AuthTokenPayload source) {
        if (source == null) {
            return new AuthTokenResponse();
        }
        return new AuthTokenResponse(
                source.getAccessToken(),
                source.getRefreshToken(),
                source.getTokenType(),
                source.getExpiresIn(),
                source.getTenantId(),
                source.getSessionId()
        );
    }
}
