package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformOrganizationRepository;
import top.kx.heartbeat.application.platform.request.PlatformDepartmentRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysDeptDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysPostDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysTenantDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class PlatformOrganizationRepositoryImpl implements PlatformOrganizationRepository {

    @Resource
    private SysDeptDOMapper deptMapper;
    @Resource
    private SysTenantDOMapper tenantMapper;
    @Resource
    private SysPostDOMapper postMapper;

    @Override
    public List<DomainRecord> listDepartments() {
        SysDeptDOExample example = new SysDeptDOExample();
        example.setOrderByClause("sort_no ASC, id ASC");
        return deptMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
    }

    @Override
    public DomainRecord createDepartment(PlatformDepartmentRequest request) {
        SysDeptDO row = departmentRow(request);
        touch(row, true);
        deptMapper.insertSelective(row);
        return record(row);
    }

    @Override
    public DomainRecord updateDepartment(String id, PlatformDepartmentRequest request) {
        Long key = longValue(id);
        SysDeptDO row = departmentRow(request);
        row.setId(key);
        touch(row, false);
        deptMapper.updateByPrimaryKeySelective(row);
        SysDeptDO persisted = key == null ? null : deptMapper.selectByPrimaryKey(key);
        return record(persisted == null ? row : persisted);
    }

    @Override
    public void deleteDepartment(String id) {
        Long key = longValue(id);
        if (key != null) {
            deptMapper.deleteByPrimaryKey(key);
        }
    }

    @Override
    public List<DomainRecord> listTenants() {
        return tenantMapper.selectByExample(new SysTenantDOExample())
                .stream()
                .map(this::recordTenant)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainRecord> listPosts() {
        return postMapper.selectByExample(new SysPostDOExample())
                .stream()
                .map(this::recordPost)
                .collect(Collectors.toList());
    }

    private SysDeptDO departmentRow(PlatformDepartmentRequest request) {
        PlatformDepartmentRequest safeRequest =
                request == null ? new PlatformDepartmentRequest() : request;
        SysDeptDO row = new SysDeptDO();
        row.setParentId(longValue(safeRequest.getParentId()));
        row.setDeptCode(safeRequest.getDeptCode());
        row.setDeptName(safeRequest.getDeptName());
        row.setAncestors(safeRequest.getAncestors());
        row.setDeptLevel(safeRequest.getDeptLevel());
        row.setLeaderUserId(longValue(safeRequest.getLeaderUserId()));
        row.setPhone(safeRequest.getPhone());
        row.setEmail(safeRequest.getEmail());
        row.setSortNo(safeRequest.getSortNo());
        row.setStatus(safeRequest.getStatus());
        return row;
    }

    private void touch(SysDeptDO row, boolean creating) {
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

    private DomainRecord record(SysDeptDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return DomainRecord.of(values);
        }
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("parentId", row.getParentId());
        values.put("deptCode", row.getDeptCode());
        values.put("code", row.getDeptCode());
        values.put("deptName", row.getDeptName());
        values.put("name", row.getDeptName());
        values.put("ancestors", row.getAncestors());
        values.put("deptLevel", row.getDeptLevel());
        values.put("leaderUserId", row.getLeaderUserId());
        values.put("phone", row.getPhone());
        values.put("email", row.getEmail());
        values.put("sortNo", row.getSortNo());
        values.put("status", row.getStatus());
        values.put("createTime", row.getCreateTime());
        values.put("updateTime", row.getUpdateTime());
        return DomainRecord.of(values);
    }

    private DomainRecord recordTenant(SysTenantDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", row.getId());
        values.put("planId", row.getPlanId());
        values.put("tenantCode", row.getTenantCode());
        values.put("tenantName", row.getTenantName());
        values.put("tenantType", row.getTenantType());
        values.put("domain", row.getDomain());
        values.put("contactName", row.getContactName());
        values.put("contactPhone", row.getContactPhone());
        values.put("contactEmail", row.getContactEmail());
        values.put("status", row.getStatus());
        values.put("createTime", row.getCreateTime());
        return DomainRecord.of(values);
    }

    private DomainRecord recordPost(SysPostDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("postCode", row.getPostCode());
        values.put("postName", row.getPostName());
        values.put("postType", row.getPostType());
        values.put("description", row.getDescription());
        values.put("sortNo", row.getSortNo());
        values.put("status", row.getStatus());
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
