package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformMenuRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysMenuDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysMenuPermissionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysRolePermissionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserRoleDOMapper;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PlatformMenuRepositoryImpl extends AbstractPlatformRepositorySupport implements PlatformMenuRepository {

    @Resource
    private SysMenuDOMapper menuMapper;
    @Resource
    private SysUserRoleDOMapper userRoleMapper;
    @Resource
    private SysRolePermissionDOMapper rolePermissionMapper;
    @Resource
    private SysMenuPermissionDOMapper menuPermissionMapper;

    @Override
    public List<DomainRecord> listMenus() {
        SysMenuDOExample example = new SysMenuDOExample();
        example.setOrderByClause("sort_no ASC, id ASC");
        return records(menuMapper.selectByExample(example));
    }

    @Override
    public List<DomainRecord> listAuthorizedMenus(String userId) {
        Set<Long> roleIds = roleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = menuIdsByPermissionIds(permissionIdsByRoleIds(roleIds));
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysMenuDOExample menuExample = new SysMenuDOExample();
        menuExample.createCriteria().andIdIn(menuIds);
        menuExample.setOrderByClause("sort_no ASC, id ASC");
        return records(menuMapper.selectByExample(menuExample));
    }

    @Override
    public DomainRecord createMenu(Map<String, Object> command) {
        return create(menuMapper, new SysMenuDO(), command);
    }

    @Override
    public DomainRecord updateMenu(String id, Map<String, Object> command) {
        return update(menuMapper, new SysMenuDO(), id, command);
    }

    @Override
    public void deleteMenu(String id) {
        delete(menuMapper, id);
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
}
