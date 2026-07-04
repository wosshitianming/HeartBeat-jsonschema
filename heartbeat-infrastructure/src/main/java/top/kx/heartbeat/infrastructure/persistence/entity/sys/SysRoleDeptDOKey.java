package top.kx.heartbeat.infrastructure.persistence.entity.sys;

import java.util.Objects;

public class SysRoleDeptDOKey {
    private Long roleId;
    private Long deptId;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SysRoleDeptDOKey)) return false;
        SysRoleDeptDOKey that = (SysRoleDeptDOKey) o;
        return Objects.equals(roleId, that.roleId) && Objects.equals(deptId, that.deptId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, deptId);
    }
}