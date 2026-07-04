package top.kx.heartbeat.domain.user.event;

import top.kx.heartbeat.domain.common.DomainEvent;
import top.kx.heartbeat.domain.user.model.valueobject.Email;
import top.kx.heartbeat.domain.user.model.valueobject.UserId;

import java.time.Instant;

/**
 * 用户已注册事件。
 *
 * <p>在 {@code User} 聚合完成注册时登记，下游可据此发送欢迎邮件、初始化积分等（最终一致）。
 */
public final class UserRegisteredEvent implements DomainEvent {

    private final UserId userId;
    private final Email email;
    private final Instant occurredOn;

    public UserRegisteredEvent(UserId userId, Email email) {
        this.userId = userId;
        this.email = email;
        this.occurredOn = Instant.now();
    }

    public UserId userId() {
        return userId;
    }

    public Email email() {
        return email;
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}
