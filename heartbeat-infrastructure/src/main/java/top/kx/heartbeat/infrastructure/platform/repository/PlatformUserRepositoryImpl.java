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
        Long id = longValue(userId);
        if (id == null) {
            return Optional.empty();
        }
        SysUserPreferenceDOExample example = new SysUserPreferenceDOExample();
        example.createCriteria().andUserIdEqualTo(id).andPreferenceKeyEqualTo(preferenceKey);
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
        Long id = longValue(userId);
        if (id == null) {
            throw new IllegalArgumentException("Invalid user id: " + userId);
        }
        SysUserPreferenceDOExample example = new SysUserPreferenceDOExample();
        example.createCriteria().andUserIdEqualTo(id).andPreferenceKeyEqualTo(preferenceKey);
        Optional<SysUserPreferenceDO> existing = first(userPreferenceMapper.selectByExampleWithBLOBs(example));
        SysUserPreferenceDO row = existing.orElseGet(SysUserPreferenceDO::new);
        row.setTenantId(tenantId());
        row.setUserId(id);
        row.setPreferenceKey(preferenceKey);
        row.setPreferenceValue(preferenceValue);
        row.setValueType("STRING");
        touch(row, !existing.isPresent());
        if (existing.isPresent()) {
            userPreferenceMapper.updateByPrimaryKeySelective(row);
        } else {
            userPreferenceMapper.insertSelective(row);
        }
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
        PlatformUserRequest safeRequest = request == null ? new PlatformUserRequest() : request;
        SysUserDO row = new SysUserDO();
        row.setDeptId(longValue(safeRequest.getDeptId()));
        row.setUsername(safeRequest.getUsername());
        row.setNickname(safeRequest.getNickname());
        row.setRealName(safeRequest.getRealName());
        row.setEmail(safeRequest.getEmail());
        row.setPhone(safeRequest.getPhone());
        row.setAvatarUrl(safeRequest.getAvatarUrl());
        row.setPasswordHash(safeRequest.getPasswordHash());
        row.setPasswordAlgo(safeRequest.getPasswordAlgo());
        row.setPasswordUpdateTime(safeRequest.getPasswordUpdateTime());
        row.setGender(safeRequest.getGender());
        row.setUserType(safeRequest.getUserType());
        row.setStatus(safeRequest.getStatus());
        return row;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @param creating 是否为新增写入。
     */
    private void touch(SysUserDO row, boolean creating) {
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
     * @param creating 是否为新增写入。
     */
    private void touch(SysUserPreferenceDO row, boolean creating) {
        Date now = new Date();
        if (creating) {
            row.setCreateTime(now);
            row.setVersion(0);
        }
        row.setUpdateTime(now);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(SysUserDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return DomainRecord.of(values);
        }
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("deptId", row.getDeptId());
        values.put("username", row.getUsername());
        values.put("nickname", row.getNickname());
        values.put("realName", row.getRealName());
        values.put("email", row.getEmail());
        values.put("phone", row.getPhone());
        values.put("avatarUrl", row.getAvatarUrl());
        values.put("avatar", row.getAvatarUrl());
        values.put("passwordHash", row.getPasswordHash());
        values.put("passwordAlgo", row.getPasswordAlgo());
        values.put("passwordUpdateTime", row.getPasswordUpdateTime());
        values.put("gender", row.getGender());
        values.put("userType", row.getUserType());
        values.put("status", row.getStatus());
        values.put("lastLoginAt", row.getLastLoginAt());
        values.put("lastLoginIp", row.getLastLoginIp());
        values.put("createTime", row.getCreateTime());
        values.put("updateTime", row.getUpdateTime());
        return DomainRecord.of(values);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord recordPreference(SysUserPreferenceDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return DomainRecord.of(values);
        }
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("userId", row.getUserId());
        values.put("preferenceKey", row.getPreferenceKey());
        values.put("preferenceValue", row.getPreferenceValue());
        values.put("valueType", row.getValueType());
        values.put("createTime", row.getCreateTime());
        values.put("updateTime", row.getUpdateTime());
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
