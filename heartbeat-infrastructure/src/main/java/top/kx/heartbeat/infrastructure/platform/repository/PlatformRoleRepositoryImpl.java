package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformRoleRepository;
import top.kx.heartbeat.application.platform.request.PlatformRoleRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysRoleDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysRoleDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysRoleDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class PlatformRoleRepositoryImpl implements PlatformRoleRepository {

    @Resource
    private SysRoleDOMapper roleMapper;

    @Override
    public List<DomainRecord> listRoles() {
        SysRoleDOExample example = new SysRoleDOExample();
        example.setOrderByClause("sort_no ASC, id ASC");
        return roleMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
    }

    @Override
    public DomainRecord createRole(PlatformRoleRequest request) {
        SysRoleDO row = roleRow(request);
        touch(row, true);
        roleMapper.insertSelective(row);
        return record(row);
    }

    @Override
    public DomainRecord updateRole(String id, PlatformRoleRequest request) {
        Long key = longValue(id);
        SysRoleDO row = roleRow(request);
        row.setId(key);
        touch(row, false);
        roleMapper.updateByPrimaryKeySelective(row);
        SysRoleDO persisted = key == null ? null : roleMapper.selectByPrimaryKey(key);
        return record(persisted == null ? row : persisted);
    }

    @Override
    public void deleteRole(String id) {
        Long key = longValue(id);
        if (key != null) {
            roleMapper.deleteByPrimaryKey(key);
        }
    }

    private SysRoleDO roleRow(PlatformRoleRequest request) {
        PlatformRoleRequest safeRequest = request == null ? new PlatformRoleRequest() : request;
        SysRoleDO row = new SysRoleDO();
        row.setRoleCode(safeRequest.getRoleCode());
        row.setRoleName(safeRequest.getRoleName());
        row.setRoleType(safeRequest.getRoleType());
        row.setDataScope(safeRequest.getDataScope());
        row.setDescription(safeRequest.getDescription());
        row.setSortNo(safeRequest.getSortNo());
        row.setStatus(safeRequest.getStatus());
        return row;
    }

    private void touch(SysRoleDO row, boolean creating) {
        Date now = new Date();
        if (creating) {
            row.setTenantId(tenantId());
            row.setCreateTime(now);
            row.setVersion(0);
            row.setDeleteMarker(0L);
            if (row.getStatus() == null) {
                row.setStatus("ENABLED");
            }
        }
        row.setUpdateTime(now);
    }

    private DomainRecord record(SysRoleDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return DomainRecord.of(values);
        }
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("roleCode", row.getRoleCode());
        values.put("code", row.getRoleCode());
        values.put("roleName", row.getRoleName());
        values.put("name", row.getRoleName());
        values.put("roleType", row.getRoleType());
        values.put("dataScope", row.getDataScope());
        values.put("description", row.getDescription());
        values.put("sortNo", row.getSortNo());
        values.put("status", row.getStatus());
        values.put("createTime", row.getCreateTime());
        values.put("updateTime", row.getUpdateTime());
        return DomainRecord.of(values);
    }

    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
