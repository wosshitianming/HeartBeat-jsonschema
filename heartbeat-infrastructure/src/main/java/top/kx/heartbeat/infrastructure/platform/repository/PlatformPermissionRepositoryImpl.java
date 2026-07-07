// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.platform.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.platform.port.PlatformPermissionRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.*;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PlatformPermissionRepositoryImpl implements PlatformPermissionRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysUserRoleDOMapper userRoleMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysRoleDOMapper roleMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysRolePermissionDOMapper rolePermissionMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysPermissionDOMapper permissionMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysMenuPermissionDOMapper menuPermissionMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysRoleDeptDOMapper roleDeptMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<String> listPermissionsByUserId(String userId) {
        // 注释：设置或计算当前变量值。
        List<Long> permissionIds = permissionIdsByRoleIds(roleIds(userId));
        // 注释：判断当前业务条件。
        if (permissionIds.isEmpty()) {
            // 注释：返回当前处理结果。
            return Collections.emptyList();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysPermissionDOExample permissionExample = new SysPermissionDOExample();
        // 注释：执行当前代码行。
        permissionExample.createCriteria().andIdIn(permissionIds);
        // 注释：返回当前处理结果。
        return permissionMapper.selectByExample(permissionExample)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(SysPermissionDO::getPermissionCode)
                // 注释：继续当前链式调用。
                .filter(StringUtils::isNotBlank)
                // 注释：继续当前链式调用。
                .distinct()
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<String> listRoleCodesByUserId(String userId) {
        // 注释：设置或计算当前变量值。
        Set<Long> roleIds = roleIds(userId);
        // 注释：判断当前业务条件。
        if (roleIds.isEmpty()) {
            // 注释：返回当前处理结果。
            return Collections.emptyList();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysRoleDOExample example = new SysRoleDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andIdIn(new ArrayList<>(roleIds));
        // 注释：返回当前处理结果。
        return roleMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(SysRoleDO::getRoleCode)
                // 注释：继续当前链式调用。
                .filter(StringUtils::isNotBlank)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<String> listDataScopesByUserId(String userId) {
        // 注释：设置或计算当前变量值。
        Set<Long> roleIds = roleIds(userId);
        // 注释：判断当前业务条件。
        if (roleIds.isEmpty()) {
            // 注释：返回当前处理结果。
            return Collections.emptyList();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysRoleDOExample example = new SysRoleDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andIdIn(new ArrayList<>(roleIds));
        // 注释：返回当前处理结果。
        return roleMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(SysRoleDO::getDataScope)
                // 注释：继续当前链式调用。
                .filter(StringUtils::isNotBlank)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<String> listCustomDeptIdsByUserId(String userId) {
        // 注释：设置或计算当前变量值。
        Set<Long> roleIds = roleIds(userId);
        // 注释：判断当前业务条件。
        if (roleIds.isEmpty()) {
            // 注释：返回当前处理结果。
            return Collections.emptyList();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysRoleDeptDOExample example = new SysRoleDeptDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andRoleIdIn(new ArrayList<>(roleIds));
        // 注释：返回当前处理结果。
        return roleDeptMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(SysRoleDeptDO::getDeptId)
                // 注释：继续当前链式调用。
                .filter(Objects::nonNull)
                // 注释：继续当前链式调用。
                .map(String::valueOf)
                // 注释：继续当前链式调用。
                .distinct()
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public boolean roleExists(String roleId) {
        // 注释：设置或计算当前变量值。
        Long id = longValue(roleId);
        // 注释：返回当前处理结果。
        return id != null && roleMapper.selectByPrimaryKey(id) != null;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<String> listRoleMenuIds(String roleId) {
        // 注释：设置或计算当前变量值。
        Long id = longValue(roleId);
        // 注释：判断当前业务条件。
        if (id == null) {
            // 注释：返回当前处理结果。
            return Collections.emptyList();
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return menuIdsByPermissionIds(permissionIdsByRoleIds(Collections.singleton(id)))
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(String::valueOf)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public void saveRoleMenus(String roleId, List<String> menuIds) {
        // 注释：设置或计算当前变量值。
        Long id = longValue(roleId);
        // 注释：判断当前业务条件。
        if (id == null) {
            // 注释：返回当前处理结果。
            return;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysRolePermissionDOExample example = new SysRolePermissionDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andRoleIdEqualTo(id);
        // 注释：执行当前代码行。
        rolePermissionMapper.deleteByExample(example);
        // 注释：设置或计算当前变量值。
        List<Long> parsedMenuIds = menuIds.stream()
                // 注释：继续当前链式调用。
                .map(this::longValue)
                // 注释：继续当前链式调用。
                .filter(Objects::nonNull)
                // 注释：继续当前链式调用。
                .distinct()
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：判断当前业务条件。
        if (parsedMenuIds.isEmpty()) {
            // 注释：返回当前处理结果。
            return;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysMenuPermissionDOExample menuPermissionExample = new SysMenuPermissionDOExample();
        // 注释：执行当前代码行。
        menuPermissionExample.createCriteria().andMenuIdIn(parsedMenuIds);
        // 注释：设置或计算当前变量值。
        List<Long> permissionIds = menuPermissionMapper.selectByExample(menuPermissionExample)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(SysMenuPermissionDO::getPermissionId)
                // 注释：继续当前链式调用。
                .filter(Objects::nonNull)
                // 注释：继续当前链式调用。
                .distinct()
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：遍历当前数据集合。
        for (Long permissionId : permissionIds) {
            // 注释：设置或计算当前变量值。
            SysRolePermissionDO row = new SysRolePermissionDO();
            // 注释：执行当前代码行。
            row.setTenantId(tenantId());
            // 注释：执行当前代码行。
            row.setRoleId(id);
            // 注释：执行当前代码行。
            row.setPermissionId(permissionId);
            // 注释：执行当前代码行。
            row.setCreateTime(now);
            // 注释：执行当前代码行。
            row.setUpdateTime(now);
            // 注释：执行当前代码行。
            rolePermissionMapper.insertSelective(row);
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
