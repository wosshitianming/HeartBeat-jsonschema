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
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String resolvedTenantId = hasText(tenantId) ? tenantId : "1";
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String resolvedSessionId = hasText(sessionId) ? sessionId : UUID.randomUUID().toString();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String accessToken = buildToken(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                userId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                username,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                resolvedTenantId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                resolvedSessionId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                TYPE_ACCESS,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                properties.getAccessTokenMinutes() * 60_000L
        );
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String refreshToken = buildToken(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                userId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                username,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                resolvedTenantId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                resolvedSessionId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                TYPE_REFRESH,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                properties.getRefreshTokenDays() * 86_400_000L
        );

        // 返回已经完成封装的业务结果。
        return AuthTokenPayload.builder()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .accessToken(accessToken)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .refreshToken(refreshToken)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .tokenType("Bearer")
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .expiresIn(properties.getAccessTokenMinutes() * 60L)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .tenantId(resolvedTenantId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .sessionId(resolvedSessionId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
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
        // 提取第三方登录返回字段，后续用于绑定或创建本地用户。
        Claims claims = parseClaims(refreshToken);
        // 比对当前业务状态，决定是否进入该处理分支。
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE))) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Invalid refresh token");
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        long tenantId = Long.parseLong(claimAsString(claims, CLAIM_TENANT_ID, "1"));
        // 根据当前业务条件选择对应处理路径。
        if (tenantId <= 0) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Invalid tenant id in refresh token");
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String sessionId = claimAsString(claims, CLAIM_SESSION_ID, "");
        // 根据当前业务条件选择对应处理路径。
        if (!hasText(sessionId)) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Invalid session id in refresh token");
        }
        // 返回已经完成封装的业务结果。
        return AuthTokenClaims.builder()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .userId(claims.get(CLAIM_USER_ID, String.class))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .username(claims.get(CLAIM_USERNAME, String.class))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .tenantId(tenantId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .sessionId(sessionId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .tokenType(TYPE_REFRESH)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
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
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            Claims claims = parseClaims(state);
            // 返回已经完成封装的业务结果。
            return "state".equals(claims.get(CLAIM_TOKEN_TYPE));
        } catch (RuntimeException ex) {
            // 返回已经完成封装的业务结果。
            return false;
        }
    }

    @Override
    public String issueBindTicket(String provider, String openId, String nickname, String avatar) {
        // 返回已经完成封装的业务结果。
        return Jwts.builder()
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                .setId(UUID.randomUUID().toString())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .claim(CLAIM_TOKEN_TYPE, "bind")
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .claim("provider", provider)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .claim("openId", openId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .claim("nickname", nickname)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .claim("avatar", avatar)
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                .setIssuedAt(new Date())
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                .setExpiration(new Date(System.currentTimeMillis() + 600_000L))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .compact();
    }

    @Override
    public Map<String, String> parseBindTicket(String bindTicket) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Claims claims = parseClaims(bindTicket);
        // 比对当前业务状态，决定是否进入该处理分支。
        if (!"bind".equals(claims.get(CLAIM_TOKEN_TYPE))) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Bind ticket is invalid or expired");
        }
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> result = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("provider", claims.get("provider", String.class));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("openId", claims.get("openId", String.class));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("nickname", claims.get("nickname", String.class));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("avatar", claims.get("avatar", String.class));
        // 返回已经完成封装的业务结果。
        return result;
    }

    private String buildToken(
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            String userId,
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            String username,
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            String tenantId,
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            String sessionId,
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            String tokenType,
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            long ttlMs
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    ) {
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 提取第三方登录返回字段，后续用于绑定或创建本地用户。
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                .setId(UUID.randomUUID().toString())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .claim(CLAIM_USER_ID, userId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .claim(CLAIM_USERNAME, username)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                .setIssuedAt(now)
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                .setExpiration(new Date(now.getTime() + ttlMs));
        // 根据当前业务条件选择对应处理路径。
        if (hasText(tenantId)) {
            // 追加代码或文本片段，逐步生成最终内容。
            builder.claim(CLAIM_TENANT_ID, tenantId);
        }
        // 根据当前业务条件选择对应处理路径。
        if (hasText(sessionId)) {
            // 追加代码或文本片段，逐步生成最终内容。
            builder.claim(CLAIM_SESSION_ID, sessionId);
        }
        // 返回已经完成封装的业务结果。
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
