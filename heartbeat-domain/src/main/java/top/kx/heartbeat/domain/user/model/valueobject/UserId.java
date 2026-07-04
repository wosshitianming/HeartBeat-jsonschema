package top.kx.heartbeat.domain.user.model.valueobject;

import top.kx.heartbeat.domain.common.ValueObject;

import java.util.Objects;

/**
 * 用户标识值对象。
 *
 * <p>使用强类型 ID 取代裸 {@code Long}，避免不同聚合的 ID 在方法参数中被混用，提升类型安全。
 */
public final class UserId implements ValueObject {

    private final Long value;

    private UserId(Long value) {
        this.value = value;
    }

    public static UserId of(Long value) {
        Objects.requireNonNull(value, "UserId 不能为空");
        return new UserId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserId userId = (UserId) o;
        return Objects.equals(value, userId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
