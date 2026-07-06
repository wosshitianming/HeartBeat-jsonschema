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

@Repository
public class PlatformMenuRepositoryImpl implements PlatformMenuRepository {

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
        return menuMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
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
        return menuMapper.selectByExample(menuExample).stream().map(this::record).collect(Collectors.toList());
    }

    @Override
    public DomainRecord createMenu(PlatformMenuRequest request) {
        SysMenuDO row = menuRow(request);
        touch(row, true);
        menuMapper.insertSelective(row);
        return record(row);
    }

    @Override
    public DomainRecord updateMenu(String id, PlatformMenuRequest request) {
        Long key = longValue(id);
        SysMenuDO row = menuRow(request);
        row.setId(key);
        touch(row, false);
        menuMapper.updateByPrimaryKeySelective(row);
        SysMenuDO persisted = key == null ? null : menuMapper.selectByPrimaryKey(key);
        return record(persisted == null ? row : persisted);
    }

    @Override
    public void deleteMenu(String id) {
        Long key = longValue(id);
        if (key != null) {
            menuMapper.deleteByPrimaryKey(key);
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

    private SysMenuDO menuRow(PlatformMenuRequest request) {
        PlatformMenuRequest safeRequest = request == null ? new PlatformMenuRequest() : request;
        SysMenuDO row = new SysMenuDO();
        row.setParentId(longValue(safeRequest.getParentId()));
        row.setMenuCode(safeRequest.getMenuCode());
        row.setMenuName(safeRequest.getMenuName());
        row.setMenuType(safeRequest.getMenuType());
        row.setRoutePath(safeRequest.getRoutePath());
        row.setComponentPath(safeRequest.getComponentPath());
        row.setRedirectPath(safeRequest.getRedirectPath());
        row.setIcon(safeRequest.getIcon());
        row.setVisible(safeRequest.getVisible());
        row.setKeepAlive(safeRequest.getKeepAlive());
        row.setExternalLink(safeRequest.getExternalLink());
        row.setPermissionMode(safeRequest.getPermissionMode());
        row.setSortNo(safeRequest.getSortNo());
        row.setStatus(safeRequest.getStatus());
        return row;
    }

    private void touch(SysMenuDO row, boolean creating) {
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

    private DomainRecord record(SysMenuDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return DomainRecord.of(values);
        }
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("parentId", row.getParentId());
        values.put("menuCode", row.getMenuCode());
        values.put("menuName", row.getMenuName());
        values.put("name", row.getMenuName());
        values.put("menuType", row.getMenuType());
        values.put("type", row.getMenuType());
        values.put("routePath", row.getRoutePath());
        values.put("path", row.getRoutePath());
        values.put("componentPath", row.getComponentPath());
        values.put("component", row.getComponentPath());
        values.put("redirectPath", row.getRedirectPath());
        values.put("icon", row.getIcon());
        values.put("visible", row.getVisible());
        values.put("keepAlive", row.getKeepAlive());
        values.put("externalLink", row.getExternalLink());
        values.put("permissionMode", row.getPermissionMode());
        values.put("permission", row.getPermissionMode());
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
