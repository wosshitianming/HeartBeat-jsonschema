package top.kx.heartbeat.application.auth;


import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.auth.response.AuthTokenResponse;
import top.kx.heartbeat.domain.auth.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthenticationSessionService {

    @Resource
    private AuthSessionRepository authSessionRepository;
    @Resource
    private TokenIssuer tokenIssuer;

    @Transactional
    public AuthTokenResponse createSession(String userId, String username, String tenantId) {
        long uid = positiveLong(userId, "userId");
        long tid = positiveLong(tenantId, "tenantId");
        String sessionId = UUID.randomUUID().toString();
        AuthTokenPayload tokens = tokenIssuer.issueTokens(String.valueOf(uid), username, String.valueOf(tid), sessionId);
        LocalDateTime now = LocalDateTime.now();
        authSessionRepository.create(AuthSession.builder()
                .tenantId(tid)
                .sessionId(sessionId)
                .userId(uid)
                .refreshTokenHash(sha256(tokens.getRefreshToken()))
                .status(AuthSessionStatus.ACTIVE.getCode())
                .issuedAt(now)
                .expireAt(now.plusSeconds(tokenIssuer.accessTokenTtlSeconds()))
                .refreshExpireAt(now.plusSeconds(tokenIssuer.refreshTokenTtlSeconds()))
                .lastAccessAt(now)
                .createTime(now)
                .updateTime(now)
                .build());
        return AuthTokenResponse.from(tokens);
    }

    @Transactional
    public AuthSession requireActive(long tenantId, String userId, String sessionId) {
        long uid = positiveLong(userId, "userId");
        LocalDateTime now = LocalDateTime.now();
        AuthSession session = authSessionRepository.findActive(tenantId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Auth session is not active"));
        if (session.getUserId() != uid || !session.isActiveAt(now)) {
            throw new IllegalArgumentException("Auth session is not active");
        }
        authSessionRepository.touch(tenantId, sessionId, now);
        return session;
    }

    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        AuthTokenClaims claims = tokenIssuer.parseRefreshToken(refreshToken);
        LocalDateTime now = LocalDateTime.now();
        AuthSession session = authSessionRepository.findActive(claims.getTenantId(), claims.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Auth session is not active"));
        if (session.getUserId() != positiveLong(claims.getUserId(), "userId") || !session.canRefreshAt(now)) {
            throw new IllegalArgumentException("Auth session cannot be refreshed");
        }
        if (!sha256(refreshToken).equals(session.getRefreshTokenHash())) {
            throw new IllegalArgumentException("Refresh token has been rotated");
        }

        AuthTokenPayload tokens = tokenIssuer.issueTokens(
                claims.getUserId(),
                claims.getUsername(),
                String.valueOf(claims.getTenantId()),
                claims.getSessionId()
        );
        authSessionRepository.rotateRefreshToken(
                claims.getTenantId(),
                claims.getSessionId(),
                sha256(tokens.getRefreshToken()),
                now.plusSeconds(tokenIssuer.refreshTokenTtlSeconds())
        );
        return AuthTokenResponse.from(tokens);
    }

    @Transactional
    public void logout(long tenantId, String sessionId) {
        if (tenantId <= 0 || !hasText(sessionId)) {
            return;
        }
        authSessionRepository.revoke(tenantId, sessionId, LocalDateTime.now());
    }

    private long positiveLong(String value, String fieldName) {
        try {
            long result = Long.parseLong(stringValue(value));
            if (result > 0) {
                return result;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException(fieldName + " must be a positive long");
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean hasText(String value) {
        return StringUtils.isNotBlank(value);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(stringValue(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
