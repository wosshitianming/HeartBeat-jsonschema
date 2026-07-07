// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformMenuRepository;
import top.kx.heartbeat.application.platform.request.PlatformMenuRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysMenuDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysMenuPermissionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysRolePermissionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserRoleDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PlatformMenuRepositoryImpl implements PlatformMenuRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysMenuDOMapper menuMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysUserRoleDOMapper userRoleMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysRolePermissionDOMapper rolePermissionMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysMenuPermissionDOMapper menuPermissionMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listMenus() {
        // 注释：设置或计算当前变量值。
        SysMenuDOExample example = new SysMenuDOExample();
        // 注释：执行当前代码行。
        example.setOrderByClause("sort_no ASC, id ASC");
        // 注释：返回当前处理结果。
        return menuMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listAuthorizedMenus(String userId) {
        // 注释：设置或计算当前变量值。
        Set<Long> roleIds = roleIds(userId);
        // 注释：判断当前业务条件。
        if (roleIds.isEmpty()) {
            // 注释：返回当前处理结果。
            return Collections.emptyList();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        List<Long> menuIds = menuIdsByPermissionIds(permissionIdsByRoleIds(roleIds));
        // 注释：判断当前业务条件。
        if (menuIds.isEmpty()) {
            // 注释：返回当前处理结果。
            return Collections.emptyList();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysMenuDOExample menuExample = new SysMenuDOExample();
        // 注释：执行当前代码行。
        menuExample.createCriteria().andIdIn(menuIds);
        // 注释：执行当前代码行。
        menuExample.setOrderByClause("sort_no ASC, id ASC");
        // 注释：返回当前处理结果。
        return menuMapper.selectByExample(menuExample).stream().map(this::record).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createMenu(PlatformMenuRequest request) {
        // 注释：设置或计算当前变量值。
        SysMenuDO row = menuRow(request);
        // 注释：执行当前代码行。
        touch(row, true);
        // 注释：执行当前代码行。
        menuMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return record(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord updateMenu(String id, PlatformMenuRequest request) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：设置或计算当前变量值。
        SysMenuDO row = menuRow(request);
        // 注释：执行当前代码行。
        row.setId(key);
        // 注释：执行当前代码行。
        touch(row, false);
        // 注释：执行当前代码行。
        menuMapper.updateByPrimaryKeySelective(row);
        // 注释：设置或计算当前变量值。
        SysMenuDO persisted = key == null ? null : menuMapper.selectByPrimaryKey(key);
        // 注释：返回当前处理结果。
        return record(persisted == null ? row : persisted);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public void deleteMenu(String id) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：判断当前业务条件。
        if (key != null) {
            // 注释：执行当前代码行。
            menuMapper.deleteByPrimaryKey(key);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Set<Long> roleIds(String userId) {
        // 注释：设置或计算当前变量值。
        Long id = longValue(userId);
        // 注释：判断当前业务条件。
        if (id == null) {
            // 注释：返回当前处理结果。
            return Collections.emptySet();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysUserRoleDOExample example = new SysUserRoleDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andUserIdEqualTo(id);
        // 注释：返回当前处理结果。
        return userRoleMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(SysUserRoleDOKey::getRoleId)
                // 注释：继续当前链式调用。
                .filter(Objects::nonNull)
                // 注释：继续当前链式调用。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Long> permissionIdsByRoleIds(Set<Long> roleIds) {
        // 注释：判断当前业务条件。
        if (roleIds == null || roleIds.isEmpty()) {
            // 注释：返回当前处理结果。
            return Collections.emptyList();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysRolePermissionDOExample example = new SysRolePermissionDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andRoleIdIn(new ArrayList<>(roleIds));
        // 注释：返回当前处理结果。
        return rolePermissionMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(SysRolePermissionDO::getPermissionId)
                // 注释：继续当前链式调用。
                .filter(Objects::nonNull)
                // 注释：继续当前链式调用。
                .distinct()
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Long> menuIdsByPermissionIds(List<Long> permissionIds) {
        // 注释：判断当前业务条件。
        if (permissionIds == null || permissionIds.isEmpty()) {
            // 注释：返回当前处理结果。
            return Collections.emptyList();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysMenuPermissionDOExample example = new SysMenuPermissionDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andPermissionIdIn(permissionIds);
        // 注释：返回当前处理结果。
        return menuPermissionMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(SysMenuPermissionDO::getMenuId)
                // 注释：继续当前链式调用。
                .filter(Objects::nonNull)
                // 注释：继续当前链式调用。
                .distinct()
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private SysMenuDO menuRow(PlatformMenuRequest request) {
        // 注释：设置或计算当前变量值。
        PlatformMenuRequest safeRequest = request == null ? new PlatformMenuRequest() : request;
        // 注释：设置或计算当前变量值。
        SysMenuDO row = new SysMenuDO();
        // 注释：执行当前代码行。
        row.setParentId(longValue(safeRequest.getParentId()));
        // 注释：执行当前代码行。
        row.setMenuCode(safeRequest.getMenuCode());
        // 注释：执行当前代码行。
        row.setMenuName(safeRequest.getMenuName());
        // 注释：执行当前代码行。
        row.setMenuType(safeRequest.getMenuType());
        // 注释：执行当前代码行。
        row.setRoutePath(safeRequest.getRoutePath());
        // 注释：执行当前代码行。
        row.setComponentPath(safeRequest.getComponentPath());
        // 注释：执行当前代码行。
        row.setRedirectPath(safeRequest.getRedirectPath());
        // 注释：执行当前代码行。
        row.setIcon(safeRequest.getIcon());
        // 注释：执行当前代码行。
        row.setVisible(safeRequest.getVisible());
        // 注释：执行当前代码行。
        row.setKeepAlive(safeRequest.getKeepAlive());
        // 注释：执行当前代码行。
        row.setExternalLink(safeRequest.getExternalLink());
        // 注释：执行当前代码行。
        row.setPermissionMode(safeRequest.getPermissionMode());
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
    private void touch(SysMenuDO row, boolean creating) {
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
    private DomainRecord record(SysMenuDO row) {
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
        values.put("menuCode", row.getMenuCode());
        // 注释：执行当前代码行。
        values.put("menuName", row.getMenuName());
        // 注释：执行当前代码行。
        values.put("name", row.getMenuName());
        // 注释：执行当前代码行。
        values.put("menuType", row.getMenuType());
        // 注释：执行当前代码行。
        values.put("type", row.getMenuType());
        // 注释：执行当前代码行。
        values.put("routePath", row.getRoutePath());
        // 注释：执行当前代码行。
        values.put("path", row.getRoutePath());
        // 注释：执行当前代码行。
        values.put("componentPath", row.getComponentPath());
        // 注释：执行当前代码行。
        values.put("component", row.getComponentPath());
        // 注释：执行当前代码行。
        values.put("redirectPath", row.getRedirectPath());
        // 注释：执行当前代码行。
        values.put("icon", row.getIcon());
        // 注释：执行当前代码行。
        values.put("visible", row.getVisible());
        // 注释：执行当前代码行。
        values.put("keepAlive", row.getKeepAlive());
        // 注释：执行当前代码行。
        values.put("externalLink", row.getExternalLink());
        // 注释：执行当前代码行。
        values.put("permissionMode", row.getPermissionMode());
        // 注释：执行当前代码行。
        values.put("permission", row.getPermissionMode());
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
