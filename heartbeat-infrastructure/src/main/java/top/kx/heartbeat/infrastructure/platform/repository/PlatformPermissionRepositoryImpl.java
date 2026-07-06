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

@Repository
public class PlatformPermissionRepositoryImpl implements PlatformPermissionRepository {

    @Resource
    private SysUserRoleDOMapper userRoleMapper;
    @Resource
    private SysRoleDOMapper roleMapper;
    @Resource
    private SysRolePermissionDOMapper rolePermissionMapper;
    @Resource
    private SysPermissionDOMapper permissionMapper;
    @Resource
    private SysMenuPermissionDOMapper menuPermissionMapper;
    @Resource
    private SysRoleDeptDOMapper roleDeptMapper;

    @Override
    public List<String> listPermissionsByUserId(String userId) {
        List<Long> permissionIds = permissionIdsByRoleIds(roleIds(userId));
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysPermissionDOExample permissionExample = new SysPermissionDOExample();
        permissionExample.createCriteria().andIdIn(permissionIds);
        return permissionMapper.selectByExample(permissionExample)
                .stream()
                .map(SysPermissionDO::getPermissionCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listRoleCodesByUserId(String userId) {
        Set<Long> roleIds = roleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysRoleDOExample example = new SysRoleDOExample();
        example.createCriteria().andIdIn(new ArrayList<>(roleIds));
        return roleMapper.selectByExample(example)
                .stream()
                .map(SysRoleDO::getRoleCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listDataScopesByUserId(String userId) {
        Set<Long> roleIds = roleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysRoleDOExample example = new SysRoleDOExample();
        example.createCriteria().andIdIn(new ArrayList<>(roleIds));
        return roleMapper.selectByExample(example)
                .stream()
                .map(SysRoleDO::getDataScope)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listCustomDeptIdsByUserId(String userId) {
        Set<Long> roleIds = roleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysRoleDeptDOExample example = new SysRoleDeptDOExample();
        example.createCriteria().andRoleIdIn(new ArrayList<>(roleIds));
        return roleDeptMapper.selectByExample(example)
                .stream()
                .map(SysRoleDeptDO::getDeptId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean roleExists(String roleId) {
        Long id = longValue(roleId);
        return id != null && roleMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public List<String> listRoleMenuIds(String roleId) {
        Long id = longValue(roleId);
        if (id == null) {
            return Collections.emptyList();
        }
        return menuIdsByPermissionIds(permissionIdsByRoleIds(Collections.singleton(id)))
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public void saveRoleMenus(String roleId, List<String> menuIds) {
        Long id = longValue(roleId);
        if (id == null) {
            return;
        }
        SysRolePermissionDOExample example = new SysRolePermissionDOExample();
        example.createCriteria().andRoleIdEqualTo(id);
        rolePermissionMapper.deleteByExample(example);
        List<Long> parsedMenuIds = menuIds.stream()
                .map(this::longValue)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (parsedMenuIds.isEmpty()) {
            return;
        }
        SysMenuPermissionDOExample menuPermissionExample = new SysMenuPermissionDOExample();
        menuPermissionExample.createCriteria().andMenuIdIn(parsedMenuIds);
        List<Long> permissionIds = menuPermissionMapper.selectByExample(menuPermissionExample)
                .stream()
                .map(SysMenuPermissionDO::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Date now = new Date();
        for (Long permissionId : permissionIds) {
            SysRolePermissionDO row = new SysRolePermissionDO();
            row.setTenantId(tenantId());
            row.setRoleId(id);
            row.setPermissionId(permissionId);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            rolePermissionMapper.insertSelective(row);
        }
    }

    private Set<Long> roleIds(String userId) {
        Long id = longValue(userId);
        if (id == null) {
            return Collections.emptySet();
        }
        SysUserRoleDOExample example = new SysUserRoleDOExample();
        example.createCriteria().andUserIdEqualTo(id);
        return userRoleMapper.selectByExample(example)
                .stream()
                .map(SysUserRoleDOKey::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> permissionIdsByRoleIds(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysRolePermissionDOExample example = new SysRolePermissionDOExample();
        example.createCriteria().andRoleIdIn(new ArrayList<>(roleIds));
        return rolePermissionMapper.selectByExample(example)
                .stream()
                .map(SysRolePermissionDO::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> menuIdsByPermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysMenuPermissionDOExample example = new SysMenuPermissionDOExample();
        example.createCriteria().andPermissionIdIn(permissionIds);
        return menuPermissionMapper.selectByExample(example)
                .stream()
                .map(SysMenuPermissionDO::getMenuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
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
