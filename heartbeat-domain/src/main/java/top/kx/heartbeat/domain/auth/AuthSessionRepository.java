package top.kx.heartbeat.domain.auth;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 认证会话领域仓储接口
 * <p>
 * application 层通过该接口管理登录会话的创建、刷新令牌、撤销等生命周期。
 * </p>
 *
 * @author heartbeat-team
 */
public interface AuthSessionRepository {

    /**
     * 查找当前有效的会话
     *
     * @param tenantId  租户 ID
     * @param sessionId 会话 ID
     * @return 会话 Optional
     */
    Optional<AuthSession> findActive(long tenantId, String sessionId);

    /**
     * 创建一条会话记录
     *
     * @param session 会话领域模型
     * @return 已持久化的会话
     */
    AuthSession create(AuthSession session);

    /**
     * 轮换刷新令牌
     */
    void rotateRefreshToken(long tenantId, String sessionId, String refreshTokenHash,
                            LocalDateTime refreshExpireAt);

    /**
     * 更新最近访问时间
     */
    void touch(long tenantId, String sessionId, LocalDateTime lastAccessAt);

    /**
     * 撤销会话
     */
    void revoke(long tenantId, String sessionId, LocalDateTime revokedAt);
}
