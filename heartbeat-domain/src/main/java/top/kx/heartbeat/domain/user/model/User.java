package top.kx.heartbeat.domain.user.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import top.kx.heartbeat.domain.common.AggregateRoot;
import top.kx.heartbeat.domain.common.exception.DomainException;
import top.kx.heartbeat.domain.user.UserErrorCode;
import top.kx.heartbeat.domain.user.event.UserRegisteredEvent;
import top.kx.heartbeat.domain.user.model.valueobject.Email;
import top.kx.heartbeat.domain.user.model.valueobject.UserId;
import top.kx.heartbeat.domain.user.model.valueobject.UserStatus;

import java.time.Instant;
import java.util.Date;

/**
 * 用户聚合根。
 *
 * <p>聚合根是业务规则的守护者：所有状态变更都必须通过其行为方法进行，从而保证不变量始终成立。
 * 这里刻意不提供公开 setter，禁止贫血模型；对象创建通过工厂方法 {@link #register} / {@link #rehydrate}。
 *
 * <ul>
 *   <li>{@link #register} —— 业务语义上的"新注册用户"，会登记领域事件；</li>
 *   <li>{@link #rehydrate} —— 从持久化重建已有用户，不产生领域事件。</li>
 * </ul>
 */
@Getter
public class User extends AggregateRoot<UserId> {

    private UserId id;
    private String username;
    private Email email;
    private UserStatus status;
    @Setter
    private Date createTime;
    @Setter
    private Date updateTime;

    private User() {
    }

    /**
     * 注册一个新用户（业务行为，登记 {@link UserRegisteredEvent}）。
     *
     * @param id       由仓储/ID 生成策略提供的标识
     * @param username 用户名
     * @param email    邮箱（已是合法值对象）
     */
    public static User register(UserId id, String username, Email email) {
        validateUsername(username);
        User user = new User();
        user.id = id;
        user.username = username.trim();
        user.email = email;
        user.status = UserStatus.ACTIVE;
        user.createTime =new Date();
        user.updateTime = user.createTime;
        user.registerEvent(new UserRegisteredEvent(id, email));
        return user;
    }

    public static User register(String username, Email email) {
        validateUsername(username);
        User user = new User();
        user.username = username.trim();
        user.email = email;
        user.status = UserStatus.ACTIVE;
        user.createTime = new Date();
        user.updateTime = user.createTime;
        return user;
    }

    /**
     * 从持久化数据重建聚合，不触发领域事件（仅供基础设施层的仓储实现调用）。
     */
    public static User rehydrate(UserId id, String username, Email email, UserStatus status,
                                 Date createTime, Date updateTime) {
        User user = new User();
        user.id = id;
        user.username = username;
        user.email = email;
        user.status = status;
        user.createTime = createTime;
        user.updateTime = updateTime;
        return user;
    }

    /**
     * 修改邮箱。
     */
    public void changeEmail(Email newEmail) {
        if (!this.email.equals(newEmail)) {
            this.email = newEmail;
            this.updateTime = new Date();
        }
    }

    /**
     * 停用用户，体现状态流转规则：已停用的用户不可重复停用。
     */
    public void disable() {
        if (this.status == UserStatus.DISABLED) {
            throw new DomainException(UserErrorCode.USER_ALREADY_DISABLED, "用户已处于停用状态");
        }
        this.status = UserStatus.DISABLED;
        this.updateTime = new Date();
    }

    private static void validateUsername(String username) {
        if (StringUtils.length(StringUtils.trim(username)) < 2) {
            throw new DomainException(UserErrorCode.USERNAME_INVALID, "用户名长度不能少于 2 个字符");
        }
    }

}
