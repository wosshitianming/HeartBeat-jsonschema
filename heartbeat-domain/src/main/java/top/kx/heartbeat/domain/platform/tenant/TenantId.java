package top.kx.heartbeat.domain.platform.tenant;

import java.util.Objects;

public final class TenantId {

    private final long value;

    private TenantId(long value) {
        this.value = value;
    }

    public static TenantId of(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("tenant id must be positive");
        }
        return new TenantId(value);
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TenantId)) {
            return false;
        }
        TenantId tenantId = (TenantId) o;
        return value == tenantId.value;
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
