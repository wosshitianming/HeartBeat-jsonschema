package top.kx.heartbeat.domain.auth;

/**
 * Provides the authenticated user for application-layer authorization decisions.
 */
public interface CurrentUserProvider {

    String currentUserId();
}
