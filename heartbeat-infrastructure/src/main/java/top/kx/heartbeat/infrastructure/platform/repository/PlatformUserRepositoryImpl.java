package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformUserRepository;
import top.kx.heartbeat.application.platform.request.PlatformUserRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserPreferenceDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserPreferenceDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserPreferenceDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实现平台管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PlatformUserRepositoryImpl implements PlatformUserRepository {

    @Resource
    private SysUserDOMapper userMapper;
    @Resource
    private SysUserPreferenceDOMapper userPreferenceMapper;

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成平台管理数据访问。
     *
     * @param username 登录用户名。
     * @return 处理后的业务结果。
     */
    @Override
    public Optional<DomainRecord> findUserByUsername(String username) {
        SysUserDOExample example = new SysUserDOExample();
        example.createCriteria().andUsernameEqualTo(username);
        return first(userMapper.selectByExample(example)).map(this::record);
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public Optional<DomainRecord> findUserById(String userId) {
        Long id = longValue(userId);
        return id == null ? Optional.empty() : Optional.ofNullable(userMapper.selectByPrimaryKey(id)).map(this::record);
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @param preferenceKey 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @Override
    public Optional<DomainRecord> findUserPreference(String userId, String preferenceKey) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long id = longValue(userId);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (id == null) {
            // 返回已经完成封装的业务结果。
            return Optional.empty();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysUserPreferenceDOExample example = new SysUserPreferenceDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andUserIdEqualTo(id).andPreferenceKeyEqualTo(preferenceKey);
        // 返回已经完成封装的业务结果。
        return first(userPreferenceMapper.selectByExampleWithBLOBs(example)).map(this::recordPreference);
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成平台管理数据访问。
     *
     * @param userId 业务记录标识。
     * @param preferenceKey 业务处理所需参数。
     * @param preferenceValue 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord saveUserPreference(String userId, String preferenceKey, String preferenceValue) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long id = longValue(userId);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (id == null) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Invalid user id: " + userId);
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        SysUserPreferenceDOExample example = new SysUserPreferenceDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andUserIdEqualTo(id).andPreferenceKeyEqualTo(preferenceKey);
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        Optional<SysUserPreferenceDO> existing = first(userPreferenceMapper.selectByExampleWithBLOBs(example));
        // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
        SysUserPreferenceDO row = existing.orElseGet(SysUserPreferenceDO::new);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setTenantId(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUserId(id);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setPreferenceKey(preferenceKey);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setPreferenceValue(preferenceValue);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setValueType("STRING");
        // 补齐审计字段和默认值，保证新增与更新写入口径一致。
        touch(row, !existing.isPresent());
        // 根据历史记录是否存在，选择更新或新增处理路径。
        if (existing.isPresent()) {
            // 将当前业务变更写入持久化层，保持数据状态同步。
            userPreferenceMapper.updateByPrimaryKeySelective(row);
        } else {
            // 将当前业务变更写入持久化层，保持数据状态同步。
            userPreferenceMapper.insertSelective(row);
        }
        // 返回已经完成封装的业务结果。
        return recordPreference(row);
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listUsers() {
        SysUserDOExample example = new SysUserDOExample();
        example.setOrderByClause("create_time DESC, id DESC");
        return userMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createUser(PlatformUserRequest request) {
        SysUserDO row = userRow(request);
        touch(row, true);
        userMapper.insertSelective(row);
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
    public DomainRecord updateUser(String id, PlatformUserRequest request) {
        Long key = longValue(id);
        SysUserDO row = userRow(request);
        row.setId(key);
        touch(row, false);
        userMapper.updateByPrimaryKeySelective(row);
        SysUserDO persisted = key == null ? null : userMapper.selectByPrimaryKey(key);
        return record(persisted == null ? row : persisted);
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     */
    @Override
    public void deleteUser(String id) {
        Long key = longValue(id);
        if (key != null) {
            userMapper.deleteByPrimaryKey(key);
        }
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createSocialUser(PlatformUserRequest request) {
        return createUser(request);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private SysUserDO userRow(PlatformUserRequest request) {
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PlatformUserRequest safeRequest = request == null ? new PlatformUserRequest() : request;
        // 创建数据库记录对象，承载即将写入的业务字段。
        SysUserDO row = new SysUserDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setDeptId(longValue(safeRequest.getDeptId()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUsername(safeRequest.getUsername());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setNickname(safeRequest.getNickname());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRealName(safeRequest.getRealName());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setEmail(safeRequest.getEmail());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setPhone(safeRequest.getPhone());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setAvatarUrl(safeRequest.getAvatarUrl());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setPasswordHash(safeRequest.getPasswordHash());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setPasswordAlgo(safeRequest.getPasswordAlgo());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setPasswordUpdateTime(safeRequest.getPasswordUpdateTime());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setGender(safeRequest.getGender());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUserType(safeRequest.getUserType());
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
    private void touch(SysUserDO row, boolean creating) {
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
     * @param creating 是否为新增写入。
     */
    private void touch(SysUserPreferenceDO row, boolean creating) {
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 根据当前业务条件选择对应处理路径。
        if (creating) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setCreateTime(now);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setVersion(0);
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
    private DomainRecord record(SysUserDO row) {
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
        values.put("deptId", row.getDeptId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("username", row.getUsername());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("nickname", row.getNickname());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("realName", row.getRealName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("email", row.getEmail());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("phone", row.getPhone());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("avatarUrl", row.getAvatarUrl());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("avatar", row.getAvatarUrl());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("passwordHash", row.getPasswordHash());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("passwordAlgo", row.getPasswordAlgo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("passwordUpdateTime", row.getPasswordUpdateTime());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("gender", row.getGender());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("userType", row.getUserType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("lastLoginAt", row.getLastLoginAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("lastLoginIp", row.getLastLoginIp());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("updateTime", row.getUpdateTime());
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord recordPreference(SysUserPreferenceDO row) {
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
        values.put("userId", row.getUserId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("preferenceKey", row.getPreferenceKey());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("preferenceValue", row.getPreferenceValue());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("valueType", row.getValueType());
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
     * @param rows 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private <T> Optional<T> first(List<T> rows) {
        return rows == null || rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
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
