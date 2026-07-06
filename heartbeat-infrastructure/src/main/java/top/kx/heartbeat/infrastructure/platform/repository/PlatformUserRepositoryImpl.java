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

@Repository
public class PlatformUserRepositoryImpl implements PlatformUserRepository {

    @Resource
    private SysUserDOMapper userMapper;
    @Resource
    private SysUserPreferenceDOMapper userPreferenceMapper;

    @Override
    public Optional<DomainRecord> findUserByUsername(String username) {
        SysUserDOExample example = new SysUserDOExample();
        example.createCriteria().andUsernameEqualTo(username);
        return first(userMapper.selectByExample(example)).map(this::record);
    }

    @Override
    public Optional<DomainRecord> findUserById(String userId) {
        Long id = longValue(userId);
        return id == null ? Optional.empty() : Optional.ofNullable(userMapper.selectByPrimaryKey(id)).map(this::record);
    }

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

    @Override
    public List<DomainRecord> listUsers() {
        SysUserDOExample example = new SysUserDOExample();
        example.setOrderByClause("create_time DESC, id DESC");
        return userMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
    }

    @Override
    public DomainRecord createUser(PlatformUserRequest request) {
        SysUserDO row = userRow(request);
        touch(row, true);
        userMapper.insertSelective(row);
        return record(row);
    }

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

    @Override
    public void deleteUser(String id) {
        Long key = longValue(id);
        if (key != null) {
            userMapper.deleteByPrimaryKey(key);
        }
    }

    @Override
    public DomainRecord createSocialUser(PlatformUserRequest request) {
        return createUser(request);
    }

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

    private void touch(SysUserPreferenceDO row, boolean creating) {
        Date now = new Date();
        if (creating) {
            row.setCreateTime(now);
            row.setVersion(0);
        }
        row.setUpdateTime(now);
    }

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

    private <T> Optional<T> first(List<T> rows) {
        return rows == null || rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
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
