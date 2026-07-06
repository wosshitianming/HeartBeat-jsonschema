package top.kx.heartbeat.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Issued access/refresh token payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenPayload {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String tenantId;
    private String sessionId;
}
