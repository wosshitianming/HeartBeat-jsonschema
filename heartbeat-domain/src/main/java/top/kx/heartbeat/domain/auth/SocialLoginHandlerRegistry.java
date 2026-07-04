package top.kx.heartbeat.domain.auth;

/**
 * Resolves a social login strategy by provider code.
 */
public interface SocialLoginHandlerRegistry {

    SocialLoginHandler getRequired(String provider);
}
