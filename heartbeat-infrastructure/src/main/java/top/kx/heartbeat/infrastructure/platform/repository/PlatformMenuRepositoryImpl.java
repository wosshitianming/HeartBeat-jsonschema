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
 * 实现平台管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
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

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listMenus() {
        SysMenuDOExample example = new SysMenuDOExample();
        example.setOrderByClause("sort_no ASC, id ASC");
        return menuMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
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

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createMenu(PlatformMenuRequest request) {
        SysMenuDO row = menuRow(request);
        touch(row, true);
        menuMapper.insertSelective(row);
        return record(row);
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     */
    @Override
    public void deleteMenu(String id) {
        Long key = longValue(id);
        if (key != null) {
            menuMapper.deleteByPrimaryKey(key);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param roleIds 业务处理所需参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param permissionIds 业务处理所需参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
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

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @param creating 是否为新增写入。
     */
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

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
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

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
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
