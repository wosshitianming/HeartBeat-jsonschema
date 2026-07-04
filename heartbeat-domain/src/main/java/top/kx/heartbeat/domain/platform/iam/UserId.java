package top.kx.heartbeat.domain.platform.iam;

import java.util.Objects;

public final class UserId {

    private final long value;

    private UserId(long value) {
        this.value = value;
    }

    public static UserId of(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("user id must be positive");
        }
        return new UserId(value);
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserId)) {
            return false;
        }
        UserId userId = (UserId) o;
        return value == userId.value;
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
