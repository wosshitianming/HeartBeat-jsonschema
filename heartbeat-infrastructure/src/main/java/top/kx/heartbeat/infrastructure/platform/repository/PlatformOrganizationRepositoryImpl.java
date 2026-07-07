// 注释：声明当前文件所属的包路径。
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

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PlatformOrganizationRepositoryImpl implements PlatformOrganizationRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysDeptDOMapper deptMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysTenantDOMapper tenantMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysPostDOMapper postMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listDepartments() {
        // 注释：设置或计算当前变量值。
        SysDeptDOExample example = new SysDeptDOExample();
        // 注释：执行当前代码行。
        example.setOrderByClause("sort_no ASC, id ASC");
        // 注释：返回当前处理结果。
        return deptMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createDepartment(PlatformDepartmentRequest request) {
        // 注释：设置或计算当前变量值。
        SysDeptDO row = departmentRow(request);
        // 注释：执行当前代码行。
        touch(row, true);
        // 注释：执行当前代码行。
        deptMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return record(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord updateDepartment(String id, PlatformDepartmentRequest request) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：设置或计算当前变量值。
        SysDeptDO row = departmentRow(request);
        // 注释：执行当前代码行。
        row.setId(key);
        // 注释：执行当前代码行。
        touch(row, false);
        // 注释：执行当前代码行。
        deptMapper.updateByPrimaryKeySelective(row);
        // 注释：设置或计算当前变量值。
        SysDeptDO persisted = key == null ? null : deptMapper.selectByPrimaryKey(key);
        // 注释：返回当前处理结果。
        return record(persisted == null ? row : persisted);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public void deleteDepartment(String id) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：判断当前业务条件。
        if (key != null) {
            // 注释：执行当前代码行。
            deptMapper.deleteByPrimaryKey(key);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listTenants() {
        // 注释：返回当前处理结果。
        return tenantMapper.selectByExample(new SysTenantDOExample())
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::recordTenant)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listPosts() {
        // 注释：返回当前处理结果。
        return postMapper.selectByExample(new SysPostDOExample())
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::recordPost)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private SysDeptDO departmentRow(PlatformDepartmentRequest request) {
        // 注释：设置或计算当前变量值。
        PlatformDepartmentRequest safeRequest =
                // 注释：设置或计算当前变量值。
                request == null ? new PlatformDepartmentRequest() : request;
        // 注释：设置或计算当前变量值。
        SysDeptDO row = new SysDeptDO();
        // 注释：执行当前代码行。
        row.setParentId(longValue(safeRequest.getParentId()));
        // 注释：执行当前代码行。
        row.setDeptCode(safeRequest.getDeptCode());
        // 注释：执行当前代码行。
        row.setDeptName(safeRequest.getDeptName());
        // 注释：执行当前代码行。
        row.setAncestors(safeRequest.getAncestors());
        // 注释：执行当前代码行。
        row.setDeptLevel(safeRequest.getDeptLevel());
        // 注释：执行当前代码行。
        row.setLeaderUserId(longValue(safeRequest.getLeaderUserId()));
        // 注释：执行当前代码行。
        row.setPhone(safeRequest.getPhone());
        // 注释：执行当前代码行。
        row.setEmail(safeRequest.getEmail());
        // 注释：执行当前代码行。
        row.setSortNo(safeRequest.getSortNo());
        // 注释：执行当前代码行。
        row.setStatus(safeRequest.getStatus());
        // 注释：返回当前处理结果。
        return row;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void touch(SysDeptDO row, boolean creating) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：判断当前业务条件。
        if (creating) {
            // 注释：执行当前代码行。
            row.setTenantId(tenantId());
            // 注释：执行当前代码行。
            row.setCreateTime(now);
            // 注释：执行当前代码行。
            row.setVersion(0);
            // 注释：执行当前代码行。
            row.setDeleteMarker(0L);
            // 注释：判断当前业务条件。
            if (row.getStatus() == null) {
                // 注释：执行当前代码行。
                row.setStatus("ENABLED");
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        row.setUpdateTime(now);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(SysDeptDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：判断当前业务条件。
        if (row == null) {
            // 注释：返回当前处理结果。
            return DomainRecord.of(values);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("parentId", row.getParentId());
        // 注释：执行当前代码行。
        values.put("deptCode", row.getDeptCode());
        // 注释：执行当前代码行。
        values.put("code", row.getDeptCode());
        // 注释：执行当前代码行。
        values.put("deptName", row.getDeptName());
        // 注释：执行当前代码行。
        values.put("name", row.getDeptName());
        // 注释：执行当前代码行。
        values.put("ancestors", row.getAncestors());
        // 注释：执行当前代码行。
        values.put("deptLevel", row.getDeptLevel());
        // 注释：执行当前代码行。
        values.put("leaderUserId", row.getLeaderUserId());
        // 注释：执行当前代码行。
        values.put("phone", row.getPhone());
        // 注释：执行当前代码行。
        values.put("email", row.getEmail());
        // 注释：执行当前代码行。
        values.put("sortNo", row.getSortNo());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：执行当前代码行。
        values.put("updateTime", row.getUpdateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord recordTenant(SysTenantDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("planId", row.getPlanId());
        // 注释：执行当前代码行。
        values.put("tenantCode", row.getTenantCode());
        // 注释：执行当前代码行。
        values.put("tenantName", row.getTenantName());
        // 注释：执行当前代码行。
        values.put("tenantType", row.getTenantType());
        // 注释：执行当前代码行。
        values.put("domain", row.getDomain());
        // 注释：执行当前代码行。
        values.put("contactName", row.getContactName());
        // 注释：执行当前代码行。
        values.put("contactPhone", row.getContactPhone());
        // 注释：执行当前代码行。
        values.put("contactEmail", row.getContactEmail());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord recordPost(SysPostDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("postCode", row.getPostCode());
        // 注释：执行当前代码行。
        values.put("postName", row.getPostName());
        // 注释：执行当前代码行。
        values.put("postType", row.getPostType());
        // 注释：执行当前代码行。
        values.put("description", row.getDescription());
        // 注释：执行当前代码行。
        values.put("sortNo", row.getSortNo());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Long longValue(Object value) {
        // 注释：判断当前业务条件。
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return Long.parseLong(String.valueOf(value).trim());
            // 注释：捕获并处理当前异常。
        } catch (NumberFormatException ignored) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
