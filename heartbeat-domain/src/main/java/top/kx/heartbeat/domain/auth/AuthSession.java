package top.kx.heartbeat.domain.auth;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class AuthSession {
    Long id;
    long tenantId;
    String sessionId;
    long userId;
    String refreshTokenHash;
    String status;
    LocalDateTime issuedAt;
    LocalDateTime expireAt;
    LocalDateTime refreshExpireAt;
    LocalDateTime revokedAt;
    LocalDateTime lastAccessAt;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public boolean isActiveAt(LocalDateTime now) {
        // 活跃状态必须由枚举统一判断。
        return AuthSessionStatus.ACTIVE.getCode().equals(status)
                && revokedAt == null
                && expireAt != null
                && !expireAt.isBefore(now);
    }

    public boolean canRefreshAt(LocalDateTime now) {
        // 刷新令牌也只允许活跃会话使用。
        return AuthSessionStatus.ACTIVE.getCode().equals(status)
                && revokedAt == null
                && refreshExpireAt != null
                && !refreshExpireAt.isBefore(now);
    }
}
