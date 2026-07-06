package top.kx.heartbeat.domain.auth;

import java.util.Map;

/**
 * Token issuing port used by the application layer.
 */
public interface TokenIssuer {

    /** Issue an access/refresh token pair for a user. */
    AuthTokenPayload issueTokens(String userId, String username);

    /** Issue an access/refresh token pair for a tenant-scoped server session. */
    default AuthTokenPayload issueTokens(String userId, String username, String tenantId, String sessionId) {
        return issueTokens(userId, username);
    }

    /** Exchange a refresh token for a new access/refresh token pair. */
    AuthTokenPayload refreshTokens(String refreshToken);

    /** Parse and validate a refresh token without mutating server-side session state. */
    default AuthTokenClaims parseRefreshToken(String refreshToken) {
        throw new UnsupportedOperationException("Refresh token parsing is not implemented");
    }

    /** Access token lifetime used when creating server-side session records. */
    default long accessTokenTtlSeconds() {
        return 120L * 60L;
    }

    /** Refresh token lifetime used when creating server-side session records. */
    default long refreshTokenTtlSeconds() {
        return 7L * 24L * 60L * 60L;
    }

    /** Parse the user id from an access token. */
    String parseUserId(String accessToken);

    /** Parse the tenant id from an access token. */
    default long parseTenantId(String accessToken) {
        return 1L;
    }

    /** Parse the server session id from an access token. */
    default String parseSessionId(String accessToken) {
        return "";
    }

    /** Issue an OAuth state token. */
    String issueSocialState();

    /** Validate an OAuth state token. */
    boolean validateSocialState(String state);

    /** Issue a short-lived social-account bind ticket. */
    String issueBindTicket(String provider, String openId, String nickname, String avatar);

    /** Parse a social-account bind ticket. */
    Map<String, String> parseBindTicket(String bindTicket);
}
