package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformMenuRepository;
import top.kx.heartbeat.application.platform.request.PlatformMenuRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.*;
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
    @Resource
    private SysRoleDOMapper roleMapper;
    @Resource
    private SysPermissionDOMapper permissionMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listMenus() {
        SysMenuDOExample example = new SysMenuDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andDeleteMarkerEqualTo(0L);
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
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Set<Long> roleIds = roleIds(userId);
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (roleIds.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return Collections.emptyList();
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        List<Long> menuIds = menuIdsByPermissionIds(permissionIdsByRoleIds(roleIds));
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (menuIds.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return Collections.emptyList();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysMenuDOExample menuExample = new SysMenuDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        menuExample.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andIdIn(menuIds)
                .andStatusEqualTo("ENABLED")
                .andDeleteMarkerEqualTo(0L);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        menuExample.setOrderByClause("sort_no ASC, id ASC");
        // 返回已经完成封装的业务结果。
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
        validateParentMenu(request == null ? null : request.getParentId(), null);
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
        if (key == null) {
            throw new IllegalArgumentException("Invalid menu id: " + id);
        }
        validateParentMenu(request == null ? null : request.getParentId(), key);
        SysMenuDO row = menuRow(request);
        row.setId(key);
        touch(row, false);
        SysMenuDO persisted = null;
        if (key != null) {
            SysMenuDOExample example = menuById(key);
            if (menuMapper.updateByExampleSelective(row, example) == 0) {
                throw new IllegalArgumentException("Menu does not exist: " + id);
            }
            persisted = first(menuMapper.selectByExample(example));
        }
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
            menuMapper.deleteByExample(menuById(key));
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    private Set<Long> roleIds(String userId) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long id = longValue(userId);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (id == null) {
            // 返回已经完成封装的业务结果。
            return Collections.emptySet();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysUserRoleDOExample example = new SysUserRoleDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andUserIdEqualTo(id);
        Set<Long> assignedRoleIds = userRoleMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(SysUserRoleDO::getRoleId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .filter(Objects::nonNull)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (assignedRoleIds.isEmpty()) {
            return Collections.emptySet();
        }
        SysRoleDOExample roleExample = new SysRoleDOExample();
        roleExample.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andIdIn(new ArrayList<>(assignedRoleIds))
                .andStatusEqualTo("ENABLED")
                .andDeleteMarkerEqualTo(0L);
        return roleMapper.selectByExample(roleExample).stream()
                .map(SysRoleDO::getId)
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
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (roleIds == null || roleIds.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return Collections.emptyList();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysRolePermissionDOExample example = new SysRolePermissionDOExample();
        // 创建结果集合，承接后续逐项组装的数据。
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andRoleIdIn(new ArrayList<>(roleIds));
        List<Long> assignedPermissionIds = rolePermissionMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(SysRolePermissionDO::getPermissionId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .filter(Objects::nonNull)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .distinct()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
        if (assignedPermissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysPermissionDOExample permissionExample = new SysPermissionDOExample();
        permissionExample.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andIdIn(assignedPermissionIds)
                .andStatusEqualTo("ENABLED")
                .andDeleteMarkerEqualTo(0L);
        return permissionMapper.selectByExample(permissionExample).stream()
                .map(SysPermissionDO::getId)
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
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (permissionIds == null || permissionIds.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return Collections.emptyList();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysMenuPermissionDOExample example = new SysMenuPermissionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andPermissionIdIn(permissionIds);
        // 返回已经完成封装的业务结果。
        return menuPermissionMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(SysMenuPermissionDO::getMenuId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .filter(Objects::nonNull)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .distinct()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private SysMenuDO menuRow(PlatformMenuRequest request) {
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PlatformMenuRequest safeRequest = request == null ? new PlatformMenuRequest() : request;
        // 创建数据库记录对象，承载即将写入的业务字段。
        SysMenuDO row = new SysMenuDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setParentId(parentIdValue(safeRequest.getParentId()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setMenuCode(safeRequest.getMenuCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setMenuName(safeRequest.getMenuName());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setMenuType(safeRequest.getMenuType());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRoutePath(safeRequest.getRoutePath());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setComponentPath(safeRequest.getComponentPath());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRedirectPath(safeRequest.getRedirectPath());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setIcon(safeRequest.getIcon());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setVisible(safeRequest.getVisible());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setKeepAlive(safeRequest.getKeepAlive());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setExternalLink(safeRequest.getExternalLink());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setPermissionMode(safeRequest.getPermissionMode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setSortNo(safeRequest.getSortNo());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setStatus(safeRequest.getStatus());
        // 返回已经完成封装的业务结果。
        return row;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @param creating 是否为新增写入。
     */
    private void touch(SysMenuDO row, boolean creating) {
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 根据当前业务条件选择对应处理路径。
        if (creating) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setTenantId(tenantId());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setCreateTime(now);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setVersion(0);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setDeleteMarker(0L);
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (row.getStatus() == null) {
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                row.setStatus("ENABLED");
            }
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(now);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(SysMenuDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (row == null) {
            // 返回已经完成封装的业务结果。
            return DomainRecord.of(values);
        }
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("parentId", row.getParentId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("menuCode", row.getMenuCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("menuName", row.getMenuName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("name", row.getMenuName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("menuType", row.getMenuType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("type", row.getMenuType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("routePath", row.getRoutePath());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("path", row.getRoutePath());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("componentPath", row.getComponentPath());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("component", row.getComponentPath());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("redirectPath", row.getRedirectPath());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("icon", row.getIcon());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("visible", row.getVisible());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("keepAlive", row.getKeepAlive());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("externalLink", row.getExternalLink());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("permissionMode", row.getPermissionMode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("permission", row.getPermissionMode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("sortNo", row.getSortNo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("updateTime", row.getUpdateTime());
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        return TenantContext.getRequiredTenantId();
    }

    private SysMenuDOExample menuById(Long id) {
        SysMenuDOExample example = new SysMenuDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andIdEqualTo(id)
                .andDeleteMarkerEqualTo(0L);
        return example;
    }

    private void validateParentMenu(String parentId, Long menuId) {
        if (parentId == null || parentId.trim().isEmpty()) {
            return;
        }
        String value = parentId.trim();
        if ("root".equalsIgnoreCase(value) || "0".equals(value)) {
            return;
        }
        Long parentKey = longValue(value);
        if (parentKey == null || parentKey < 0L) {
            throw new IllegalArgumentException("Invalid parent menu id: " + parentId);
        }
        if (parentKey == 0L) {
            return;
        }
        if (parentKey.equals(menuId)) {
            throw new IllegalArgumentException("Menu cannot be its own parent: " + parentId);
        }
        if (menuMapper.countByExample(menuById(parentKey)) == 0L) {
            throw new IllegalArgumentException("Parent menu does not belong to current tenant: " + parentId);
        }
    }

    private Long parentIdValue(String parentId) {
        if (parentId == null || parentId.trim().isEmpty()) {
            return null;
        }
        String value = parentId.trim();
        return "root".equalsIgnoreCase(value) ? 0L : longValue(value);
    }

    private <T> T first(List<T> rows) {
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private Long longValue(Object value) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            // 返回已经完成封装的业务结果。
            return null;
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
            return null;
        }
    }
}
