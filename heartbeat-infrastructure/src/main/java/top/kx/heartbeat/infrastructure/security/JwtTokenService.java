package top.kx.heartbeat.infrastructure.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.domain.auth.AuthTokenClaims;
import top.kx.heartbeat.domain.auth.AuthTokenPayload;
import top.kx.heartbeat.domain.auth.TokenIssuer;

import javax.annotation.Resource;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT implementation for tenant-scoped access tokens, refresh tokens and short-lived OAuth tickets.
 */
@Service
public class JwtTokenService implements TokenIssuer {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TENANT_ID = "tid";
    private static final String CLAIM_SESSION_ID = "sid";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Resource
    private JwtProperties properties;

    @Override
    public AuthTokenPayload issueTokens(String userId, String username) {
        return issueTokens(userId, username, "1", UUID.randomUUID().toString());
    }

    @Override
    public AuthTokenPayload issueTokens(String userId, String username, String tenantId, String sessionId) {
        String resolvedTenantId = hasText(tenantId) ? tenantId : "1";
        String resolvedSessionId = hasText(sessionId) ? sessionId : UUID.randomUUID().toString();
        String accessToken = buildToken(
                userId,
                username,
                resolvedTenantId,
                resolvedSessionId,
                TYPE_ACCESS,
                properties.getAccessTokenMinutes() * 60_000L
        );
        String refreshToken = buildToken(
                userId,
                username,
                resolvedTenantId,
                resolvedSessionId,
                TYPE_REFRESH,
                properties.getRefreshTokenDays() * 86_400_000L
        );

        return AuthTokenPayload.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(properties.getAccessTokenMinutes() * 60L)
                .tenantId(resolvedTenantId)
                .sessionId(resolvedSessionId)
                .build();
    }

    @Override
    public AuthTokenPayload refreshTokens(String refreshToken) {
        Claims claims = parseClaims(refreshToken);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE))) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        return issueTokens(
                claims.get(CLAIM_USER_ID, String.class),
                claims.get(CLAIM_USERNAME, String.class),
                claimAsString(claims, CLAIM_TENANT_ID, "1"),
                claimAsString(claims, CLAIM_SESSION_ID, UUID.randomUUID().toString())
        );
    }

    @Override
    public AuthTokenClaims parseRefreshToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE))) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        long tenantId = Long.parseLong(claimAsString(claims, CLAIM_TENANT_ID, "1"));
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Invalid tenant id in refresh token");
        }
        String sessionId = claimAsString(claims, CLAIM_SESSION_ID, "");
        if (!hasText(sessionId)) {
            throw new IllegalArgumentException("Invalid session id in refresh token");
        }
        return AuthTokenClaims.builder()
                .userId(claims.get(CLAIM_USER_ID, String.class))
                .username(claims.get(CLAIM_USERNAME, String.class))
                .tenantId(tenantId)
                .sessionId(sessionId)
                .tokenType(TYPE_REFRESH)
                .build();
    }

    @Override
    public long accessTokenTtlSeconds() {
        return properties.getAccessTokenMinutes() * 60L;
    }

    @Override
    public long refreshTokenTtlSeconds() {
        return properties.getRefreshTokenDays() * 86_400L;
    }

    @Override
    public String parseUserId(String accessToken) {
        Claims claims = parseAccessClaims(accessToken);
        return claims.get(CLAIM_USER_ID, String.class);
    }

    @Override
    public long parseTenantId(String accessToken) {
        Claims claims = parseAccessClaims(accessToken);
        long tenantId = Long.parseLong(claimAsString(claims, CLAIM_TENANT_ID, "1"));
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Invalid tenant id in access token");
        }
        return tenantId;
    }

    @Override
    public String parseSessionId(String accessToken) {
        Claims claims = parseAccessClaims(accessToken);
        return claimAsString(claims, CLAIM_SESSION_ID, "");
    }

    @Override
    public String issueSocialState() {
        return buildToken("state", "oauth", null, null, "state", 300_000L);
    }

    @Override
    public boolean validateSocialState(String state) {
        try {
            Claims claims = parseClaims(state);
            return "state".equals(claims.get(CLAIM_TOKEN_TYPE));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public String issueBindTicket(String provider, String openId, String nickname, String avatar) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .claim(CLAIM_TOKEN_TYPE, "bind")
                .claim("provider", provider)
                .claim("openId", openId)
                .claim("nickname", nickname)
                .claim("avatar", avatar)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 600_000L))
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public Map<String, String> parseBindTicket(String bindTicket) {
        Claims claims = parseClaims(bindTicket);
        if (!"bind".equals(claims.get(CLAIM_TOKEN_TYPE))) {
            throw new IllegalArgumentException("Bind ticket is invalid or expired");
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put("provider", claims.get("provider", String.class));
        result.put("openId", claims.get("openId", String.class));
        result.put("nickname", claims.get("nickname", String.class));
        result.put("avatar", claims.get("avatar", String.class));
        return result;
    }

    private String buildToken(
            String userId,
            String username,
            String tenantId,
            String sessionId,
            String tokenType,
            long ttlMs
    ) {
        Date now = new Date();
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMs));
        if (hasText(tenantId)) {
            builder.claim(CLAIM_TENANT_ID, tenantId);
        }
        if (hasText(sessionId)) {
            builder.claim(CLAIM_SESSION_ID, sessionId);
        }
        return builder.signWith(secretKey(), SignatureAlgorithm.HS256).compact();
    }

    private Claims parseAccessClaims(String token) {
        Claims claims = parseClaims(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE))) {
            throw new IllegalArgumentException("Invalid access token");
        }
        return claims;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String claimAsString(Claims claims, String name, String defaultValue) {
        Object value = claims.get(name);
        if (StringUtils.isBlank(value == null ? null : String.valueOf(value))) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private SecretKey secretKey() {
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private boolean hasText(String value) {
        return StringUtils.isNotBlank(value);
    }
}
