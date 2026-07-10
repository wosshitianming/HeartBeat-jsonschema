package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformOrganizationRepository;
import top.kx.heartbeat.application.platform.request.PlatformDepartmentRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysDeptDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysPostDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysTenantDOMapper;
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
public class PlatformOrganizationRepositoryImpl implements PlatformOrganizationRepository {

    @Resource
    private SysDeptDOMapper deptMapper;
    @Resource
    private SysTenantDOMapper tenantMapper;
    @Resource
    private SysPostDOMapper postMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listDepartments() {
        SysDeptDOExample example = new SysDeptDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andDeleteMarkerEqualTo(0L);
        example.setOrderByClause("sort_no ASC, id ASC");
        return deptMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createDepartment(PlatformDepartmentRequest request) {
        validateParentDepartment(request == null ? null : request.getParentId(), null);
        SysDeptDO row = departmentRow(request);
        touch(row, true);
        deptMapper.insertSelective(row);
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
    public DomainRecord updateDepartment(String id, PlatformDepartmentRequest request) {
        Long key = longValue(id);
        if (key == null) {
            throw new IllegalArgumentException("Invalid department id: " + id);
        }
        validateParentDepartment(request == null ? null : request.getParentId(), key);
        SysDeptDO row = departmentRow(request);
        row.setId(key);
        touch(row, false);
        SysDeptDO persisted = null;
        if (key != null) {
            SysDeptDOExample example = departmentById(key);
            if (deptMapper.updateByExampleSelective(row, example) == 0) {
                throw new IllegalArgumentException("Department does not exist: " + id);
            }
            persisted = first(deptMapper.selectByExample(example));
        }
        return record(persisted == null ? row : persisted);
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     */
    @Override
    public void deleteDepartment(String id) {
        Long key = longValue(id);
        if (key != null) {
            deptMapper.deleteByExample(departmentById(key));
        }
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listTenants() {
        SysTenantDOExample example = new SysTenantDOExample();
        example.createCriteria().andDeleteMarkerEqualTo(0L);
        // 返回已经完成封装的业务结果。
        return tenantMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::recordTenant)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listPosts() {
        SysPostDOExample example = new SysPostDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andDeleteMarkerEqualTo(0L);
        // 返回已经完成封装的业务结果。
        return postMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::recordPost)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private SysDeptDO departmentRow(PlatformDepartmentRequest request) {
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PlatformDepartmentRequest safeRequest =
                // 创建下游写入请求对象，集中承载本次业务处理结果。
                request == null ? new PlatformDepartmentRequest() : request;
        // 创建数据库记录对象，承载即将写入的业务字段。
        SysDeptDO row = new SysDeptDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setParentId(parentIdValue(safeRequest.getParentId()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setDeptCode(safeRequest.getDeptCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setDeptName(safeRequest.getDeptName());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setAncestors(safeRequest.getAncestors());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setDeptLevel(safeRequest.getDeptLevel());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setLeaderUserId(longValue(safeRequest.getLeaderUserId()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setPhone(safeRequest.getPhone());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setEmail(safeRequest.getEmail());
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
    private void touch(SysDeptDO row, boolean creating) {
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
    private DomainRecord record(SysDeptDO row) {
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
        values.put("deptCode", row.getDeptCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("code", row.getDeptCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("deptName", row.getDeptName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("name", row.getDeptName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("ancestors", row.getAncestors());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("deptLevel", row.getDeptLevel());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("leaderUserId", row.getLeaderUserId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("phone", row.getPhone());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("email", row.getEmail());
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord recordTenant(SysTenantDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("planId", row.getPlanId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantCode", row.getTenantCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantName", row.getTenantName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantType", row.getTenantType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("domain", row.getDomain());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("contactName", row.getContactName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("contactPhone", row.getContactPhone());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("contactEmail", row.getContactEmail());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord recordPost(SysPostDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("postCode", row.getPostCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("postName", row.getPostName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("postType", row.getPostType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("description", row.getDescription());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("sortNo", row.getSortNo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
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

    private SysDeptDOExample departmentById(Long id) {
        SysDeptDOExample example = new SysDeptDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andIdEqualTo(id)
                .andDeleteMarkerEqualTo(0L);
        return example;
    }

    private void validateParentDepartment(String parentId, Long departmentId) {
        if (parentId == null || parentId.trim().isEmpty()) {
            return;
        }
        String value = parentId.trim();
        if ("root".equalsIgnoreCase(value) || "0".equals(value)) {
            return;
        }
        Long parentKey = longValue(value);
        if (parentKey == null || parentKey < 0L) {
            throw new IllegalArgumentException("Invalid parent department id: " + parentId);
        }
        if (parentKey == 0L) {
            return;
        }
        if (parentKey.equals(departmentId)) {
            throw new IllegalArgumentException("Department cannot be its own parent: " + parentId);
        }
        if (deptMapper.countByExample(departmentById(parentKey)) == 0L) {
            throw new IllegalArgumentException("Parent department does not belong to current tenant: " + parentId);
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
