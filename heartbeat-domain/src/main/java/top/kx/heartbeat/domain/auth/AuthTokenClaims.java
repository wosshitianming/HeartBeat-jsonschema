package top.kx.heartbeat.domain.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthTokenClaims {
    String userId;
    String username;
    long tenantId;
    String sessionId;
    String tokenType;
}
