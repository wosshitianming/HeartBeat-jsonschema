package top.kx.heartbeat.infrastructure.platform.repository;

import org.apache.commons.lang3.StringUtils;
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
 * 实现平台管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PlatformRoleRepositoryImpl implements PlatformRoleRepository {

    @Resource
    private SysRoleDOMapper roleMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listRoles() {
        SysRoleDOExample example = new SysRoleDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andDeleteMarkerEqualTo(0L);
        example.setOrderByClause("sort_no ASC, id ASC");
        return roleMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createRole(PlatformRoleRequest request) {
        SysRoleDO row = roleRow(request);
        touch(row, true);
        roleMapper.insertSelective(row);
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
    public DomainRecord updateRole(String id, PlatformRoleRequest request) {
        Long key = longValue(id);
        if (key == null) {
            throw new IllegalArgumentException("Invalid role id: " + id);
        }
        SysRoleDO row = roleRow(request);
        row.setId(key);
        touch(row, false);
        SysRoleDO persisted = null;
        if (key != null) {
            SysRoleDOExample example = roleById(key);
            if (roleMapper.updateByExampleSelective(row, example) == 0) {
                throw new IllegalArgumentException("Role does not exist: " + id);
            }
            persisted = first(roleMapper.selectByExample(example));
        }
        return record(persisted == null ? row : persisted);
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     */
    @Override
    public void deleteRole(String id) {
        Long key = longValue(id);
        if (key != null) {
            roleMapper.deleteByExample(roleById(key));
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private SysRoleDO roleRow(PlatformRoleRequest request) {
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PlatformRoleRequest safeRequest = request == null ? new PlatformRoleRequest() : request;
        // 创建数据库记录对象，承载即将写入的业务字段。
        SysRoleDO row = new SysRoleDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRoleCode(safeRequest.getRoleCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRoleName(safeRequest.getRoleName());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRoleType(safeRequest.getRoleType());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setDataScope(safeRequest.getDataScope());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setDescription(safeRequest.getDescription());
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
    private void touch(SysRoleDO row, boolean creating) {
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
    private DomainRecord record(SysRoleDO row) {
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
        values.put("roleCode", row.getRoleCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("code", row.getRoleCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("roleName", row.getRoleName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("name", row.getRoleName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("roleType", row.getRoleType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("dataScope", row.getDataScope());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("description", row.getDescription());
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

    private SysRoleDOExample roleById(Long id) {
        SysRoleDOExample example = new SysRoleDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andIdEqualTo(id)
                .andDeleteMarkerEqualTo(0L);
        return example;
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
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
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
