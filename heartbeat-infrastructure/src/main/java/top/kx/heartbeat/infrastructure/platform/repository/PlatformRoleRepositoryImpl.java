// 注释：声明当前文件所属的包路径。
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

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PlatformRoleRepositoryImpl implements PlatformRoleRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysRoleDOMapper roleMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listRoles() {
        // 注释：设置或计算当前变量值。
        SysRoleDOExample example = new SysRoleDOExample();
        // 注释：执行当前代码行。
        example.setOrderByClause("sort_no ASC, id ASC");
        // 注释：返回当前处理结果。
        return roleMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createRole(PlatformRoleRequest request) {
        // 注释：设置或计算当前变量值。
        SysRoleDO row = roleRow(request);
        // 注释：执行当前代码行。
        touch(row, true);
        // 注释：执行当前代码行。
        roleMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return record(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord updateRole(String id, PlatformRoleRequest request) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：设置或计算当前变量值。
        SysRoleDO row = roleRow(request);
        // 注释：执行当前代码行。
        row.setId(key);
        // 注释：执行当前代码行。
        touch(row, false);
        // 注释：执行当前代码行。
        roleMapper.updateByPrimaryKeySelective(row);
        // 注释：设置或计算当前变量值。
        SysRoleDO persisted = key == null ? null : roleMapper.selectByPrimaryKey(key);
        // 注释：返回当前处理结果。
        return record(persisted == null ? row : persisted);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public void deleteRole(String id) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：判断当前业务条件。
        if (key != null) {
            // 注释：执行当前代码行。
            roleMapper.deleteByPrimaryKey(key);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private SysRoleDO roleRow(PlatformRoleRequest request) {
        // 注释：设置或计算当前变量值。
        PlatformRoleRequest safeRequest = request == null ? new PlatformRoleRequest() : request;
        // 注释：设置或计算当前变量值。
        SysRoleDO row = new SysRoleDO();
        // 注释：执行当前代码行。
        row.setRoleCode(safeRequest.getRoleCode());
        // 注释：执行当前代码行。
        row.setRoleName(safeRequest.getRoleName());
        // 注释：执行当前代码行。
        row.setRoleType(safeRequest.getRoleType());
        // 注释：执行当前代码行。
        row.setDataScope(safeRequest.getDataScope());
        // 注释：执行当前代码行。
        row.setDescription(safeRequest.getDescription());
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
    private void touch(SysRoleDO row, boolean creating) {
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
    private DomainRecord record(SysRoleDO row) {
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
        values.put("roleCode", row.getRoleCode());
        // 注释：执行当前代码行。
        values.put("code", row.getRoleCode());
        // 注释：执行当前代码行。
        values.put("roleName", row.getRoleName());
        // 注释：执行当前代码行。
        values.put("name", row.getRoleName());
        // 注释：执行当前代码行。
        values.put("roleType", row.getRoleType());
        // 注释：执行当前代码行。
        values.put("dataScope", row.getDataScope());
        // 注释：执行当前代码行。
        values.put("description", row.getDescription());
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
