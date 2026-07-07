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
        SysRoleDO row = roleRow(request);
        row.setId(key);
        touch(row, false);
        roleMapper.updateByPrimaryKeySelective(row);
        SysRoleDO persisted = key == null ? null : roleMapper.selectByPrimaryKey(key);
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
            roleMapper.deleteByPrimaryKey(key);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private SysRoleDO roleRow(PlatformRoleRequest request) {
        PlatformRoleRequest safeRequest = request == null ? new PlatformRoleRequest() : request;
        SysRoleDO row = new SysRoleDO();
        row.setRoleCode(safeRequest.getRoleCode());
        row.setRoleName(safeRequest.getRoleName());
        row.setRoleType(safeRequest.getRoleType());
        row.setDataScope(safeRequest.getDataScope());
        row.setDescription(safeRequest.getDescription());
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
    private void touch(SysRoleDO row, boolean creating) {
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
    private DomainRecord record(SysRoleDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return DomainRecord.of(values);
        }
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("roleCode", row.getRoleCode());
        values.put("code", row.getRoleCode());
        values.put("roleName", row.getRoleName());
        values.put("name", row.getRoleName());
        values.put("roleType", row.getRoleType());
        values.put("dataScope", row.getDataScope());
        values.put("description", row.getDescription());
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
