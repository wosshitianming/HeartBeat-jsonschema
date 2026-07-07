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
 * 实现平台管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
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

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public List<String> listPermissionsByUserId(String userId) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        List<Long> permissionIds = permissionIdsByRoleIds(roleIds(userId));
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (permissionIds.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return Collections.emptyList();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysPermissionDOExample permissionExample = new SysPermissionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        permissionExample.createCriteria().andIdIn(permissionIds);
        // 返回已经完成封装的业务结果。
        return permissionMapper.selectByExample(permissionExample)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(SysPermissionDO::getPermissionCode)
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                .filter(StringUtils::isNotBlank)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .distinct()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public List<String> listRoleCodesByUserId(String userId) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Set<Long> roleIds = roleIds(userId);
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (roleIds.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return Collections.emptyList();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysRoleDOExample example = new SysRoleDOExample();
        // 创建结果集合，承接后续逐项组装的数据。
        example.createCriteria().andIdIn(new ArrayList<>(roleIds));
        // 返回已经完成封装的业务结果。
        return roleMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(SysRoleDO::getRoleCode)
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                .filter(StringUtils::isNotBlank)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public List<String> listDataScopesByUserId(String userId) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Set<Long> roleIds = roleIds(userId);
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (roleIds.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return Collections.emptyList();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysRoleDOExample example = new SysRoleDOExample();
        // 创建结果集合，承接后续逐项组装的数据。
        example.createCriteria().andIdIn(new ArrayList<>(roleIds));
        // 返回已经完成封装的业务结果。
        return roleMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(SysRoleDO::getDataScope)
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                .filter(StringUtils::isNotBlank)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public List<String> listCustomDeptIdsByUserId(String userId) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Set<Long> roleIds = roleIds(userId);
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (roleIds.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return Collections.emptyList();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysRoleDeptDOExample example = new SysRoleDeptDOExample();
        // 创建结果集合，承接后续逐项组装的数据。
        example.createCriteria().andRoleIdIn(new ArrayList<>(roleIds));
        // 返回已经完成封装的业务结果。
        return roleDeptMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(SysRoleDeptDO::getDeptId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .filter(Objects::nonNull)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(String::valueOf)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .distinct()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param roleId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public boolean roleExists(String roleId) {
        Long id = longValue(roleId);
        return id != null && roleMapper.selectByPrimaryKey(id) != null;
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @param roleId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public List<String> listRoleMenuIds(String roleId) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long id = longValue(roleId);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (id == null) {
            // 返回已经完成封装的业务结果。
            return Collections.emptyList();
        }
        // 返回已经完成封装的业务结果。
        return menuIdsByPermissionIds(permissionIdsByRoleIds(Collections.singleton(id)))
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(String::valueOf)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成平台管理数据访问。
     *
     * @param roleId 业务记录标识。
     * @param menuIds 业务处理所需参数。
     */
    @Override
    public void saveRoleMenus(String roleId, List<String> menuIds) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long id = longValue(roleId);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (id == null) {
            // 返回已经完成封装的业务结果。
            return;
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysRolePermissionDOExample example = new SysRolePermissionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andRoleIdEqualTo(id);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        rolePermissionMapper.deleteByExample(example);
        // 使用流式转换批量映射数据，减少中间状态暴露。
        List<Long> parsedMenuIds = menuIds.stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::longValue)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .filter(Objects::nonNull)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .distinct()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (parsedMenuIds.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return;
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysMenuPermissionDOExample menuPermissionExample = new SysMenuPermissionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        menuPermissionExample.createCriteria().andMenuIdIn(parsedMenuIds);
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        List<Long> permissionIds = menuPermissionMapper.selectByExample(menuPermissionExample)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(SysMenuPermissionDO::getPermissionId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .filter(Objects::nonNull)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .distinct()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Long permissionId : permissionIds) {
            // 创建数据库记录对象，承载即将写入的业务字段。
            SysRolePermissionDO row = new SysRolePermissionDO();
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setTenantId(tenantId());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setRoleId(id);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setPermissionId(permissionId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setCreateTime(now);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setUpdateTime(now);
            // 将当前业务变更写入持久化层，保持数据状态同步。
            rolePermissionMapper.insertSelective(row);
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
        example.createCriteria().andUserIdEqualTo(id);
        // 返回已经完成封装的业务结果。
        return userRoleMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(SysUserRoleDOKey::getRoleId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .filter(Objects::nonNull)
                // 使用流式转换批量映射数据，减少中间状态暴露。
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
        example.createCriteria().andRoleIdIn(new ArrayList<>(roleIds));
        // 返回已经完成封装的业务结果。
        return rolePermissionMapper.selectByExample(example)
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
        example.createCriteria().andPermissionIdIn(permissionIds);
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
