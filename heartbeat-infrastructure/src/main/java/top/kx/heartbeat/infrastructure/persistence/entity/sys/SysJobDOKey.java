package top.kx.heartbeat.infrastructure.persistence.entity.sys;

import java.util.Objects;

public class SysJobDOKey {
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SysJobDOKey)) return false;
        SysJobDOKey that = (SysJobDOKey) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
