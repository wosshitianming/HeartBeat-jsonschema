// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PlatformUserRepositoryImpl implements PlatformUserRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysUserDOMapper userMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysUserPreferenceDOMapper userPreferenceMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public Optional<DomainRecord> findUserByUsername(String username) {
        // 注释：设置或计算当前变量值。
        SysUserDOExample example = new SysUserDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andUsernameEqualTo(username);
        // 注释：返回当前处理结果。
        return first(userMapper.selectByExample(example)).map(this::record);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public Optional<DomainRecord> findUserById(String userId) {
        // 注释：设置或计算当前变量值。
        Long id = longValue(userId);
        // 注释：返回当前处理结果。
        return id == null ? Optional.empty() : Optional.ofNullable(userMapper.selectByPrimaryKey(id)).map(this::record);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public Optional<DomainRecord> findUserPreference(String userId, String preferenceKey) {
        // 注释：设置或计算当前变量值。
        Long id = longValue(userId);
        // 注释：判断当前业务条件。
        if (id == null) {
            // 注释：返回当前处理结果。
            return Optional.empty();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysUserPreferenceDOExample example = new SysUserPreferenceDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andUserIdEqualTo(id).andPreferenceKeyEqualTo(preferenceKey);
        // 注释：返回当前处理结果。
        return first(userPreferenceMapper.selectByExampleWithBLOBs(example)).map(this::recordPreference);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord saveUserPreference(String userId, String preferenceKey, String preferenceValue) {
        // 注释：设置或计算当前变量值。
        Long id = longValue(userId);
        // 注释：判断当前业务条件。
        if (id == null) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Invalid user id: " + userId);
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        SysUserPreferenceDOExample example = new SysUserPreferenceDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andUserIdEqualTo(id).andPreferenceKeyEqualTo(preferenceKey);
        // 注释：设置或计算当前变量值。
        Optional<SysUserPreferenceDO> existing = first(userPreferenceMapper.selectByExampleWithBLOBs(example));
        // 注释：设置或计算当前变量值。
        SysUserPreferenceDO row = existing.orElseGet(SysUserPreferenceDO::new);
        // 注释：执行当前代码行。
        row.setTenantId(tenantId());
        // 注释：执行当前代码行。
        row.setUserId(id);
        // 注释：执行当前代码行。
        row.setPreferenceKey(preferenceKey);
        // 注释：执行当前代码行。
        row.setPreferenceValue(preferenceValue);
        // 注释：执行当前代码行。
        row.setValueType("STRING");
        // 注释：执行当前代码行。
        touch(row, !existing.isPresent());
        // 注释：判断当前业务条件。
        if (existing.isPresent()) {
            // 注释：执行当前代码行。
            userPreferenceMapper.updateByPrimaryKeySelective(row);
            // 注释：处理条件不满足时的分支。
        } else {
            // 注释：执行当前代码行。
            userPreferenceMapper.insertSelective(row);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return recordPreference(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listUsers() {
        // 注释：设置或计算当前变量值。
        SysUserDOExample example = new SysUserDOExample();
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return userMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createUser(PlatformUserRequest request) {
        // 注释：设置或计算当前变量值。
        SysUserDO row = userRow(request);
        // 注释：执行当前代码行。
        touch(row, true);
        // 注释：执行当前代码行。
        userMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return record(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord updateUser(String id, PlatformUserRequest request) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：设置或计算当前变量值。
        SysUserDO row = userRow(request);
        // 注释：执行当前代码行。
        row.setId(key);
        // 注释：执行当前代码行。
        touch(row, false);
        // 注释：执行当前代码行。
        userMapper.updateByPrimaryKeySelective(row);
        // 注释：设置或计算当前变量值。
        SysUserDO persisted = key == null ? null : userMapper.selectByPrimaryKey(key);
        // 注释：返回当前处理结果。
        return record(persisted == null ? row : persisted);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public void deleteUser(String id) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：判断当前业务条件。
        if (key != null) {
            // 注释：执行当前代码行。
            userMapper.deleteByPrimaryKey(key);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createSocialUser(PlatformUserRequest request) {
        // 注释：返回当前处理结果。
        return createUser(request);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private SysUserDO userRow(PlatformUserRequest request) {
        // 注释：设置或计算当前变量值。
        PlatformUserRequest safeRequest = request == null ? new PlatformUserRequest() : request;
        // 注释：设置或计算当前变量值。
        SysUserDO row = new SysUserDO();
        // 注释：执行当前代码行。
        row.setDeptId(longValue(safeRequest.getDeptId()));
        // 注释：执行当前代码行。
        row.setUsername(safeRequest.getUsername());
        // 注释：执行当前代码行。
        row.setNickname(safeRequest.getNickname());
        // 注释：执行当前代码行。
        row.setRealName(safeRequest.getRealName());
        // 注释：执行当前代码行。
        row.setEmail(safeRequest.getEmail());
        // 注释：执行当前代码行。
        row.setPhone(safeRequest.getPhone());
        // 注释：执行当前代码行。
        row.setAvatarUrl(safeRequest.getAvatarUrl());
        // 注释：执行当前代码行。
        row.setPasswordHash(safeRequest.getPasswordHash());
        // 注释：执行当前代码行。
        row.setPasswordAlgo(safeRequest.getPasswordAlgo());
        // 注释：执行当前代码行。
        row.setPasswordUpdateTime(safeRequest.getPasswordUpdateTime());
        // 注释：执行当前代码行。
        row.setGender(safeRequest.getGender());
        // 注释：执行当前代码行。
        row.setUserType(safeRequest.getUserType());
        // 注释：执行当前代码行。
        row.setStatus(safeRequest.getStatus());
        // 注释：返回当前处理结果。
        return row;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void touch(SysUserDO row, boolean creating) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：判断当前业务条件。
        if (creating) {
            // 注释：执行当前代码行。
            row.setTenantId(tenantId());
            // 注释：执行当前代码行。
            row.setCreateTime(now);
            // 注释：执行当前代码行。
            row.setVersion(0);
            // 注释：执行当前代码行。
            row.setDeleteMarker(0L);
            // 注释：判断当前业务条件。
            if (row.getStatus() == null) {
                // 注释：执行当前代码行。
                row.setStatus("ENABLED");
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        row.setUpdateTime(now);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void touch(SysUserPreferenceDO row, boolean creating) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：判断当前业务条件。
        if (creating) {
            // 注释：执行当前代码行。
            row.setCreateTime(now);
            // 注释：执行当前代码行。
            row.setVersion(0);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        row.setUpdateTime(now);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(SysUserDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：判断当前业务条件。
        if (row == null) {
            // 注释：返回当前处理结果。
            return DomainRecord.of(values);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("deptId", row.getDeptId());
        // 注释：执行当前代码行。
        values.put("username", row.getUsername());
        // 注释：执行当前代码行。
        values.put("nickname", row.getNickname());
        // 注释：执行当前代码行。
        values.put("realName", row.getRealName());
        // 注释：执行当前代码行。
        values.put("email", row.getEmail());
        // 注释：执行当前代码行。
        values.put("phone", row.getPhone());
        // 注释：执行当前代码行。
        values.put("avatarUrl", row.getAvatarUrl());
        // 注释：执行当前代码行。
        values.put("avatar", row.getAvatarUrl());
        // 注释：执行当前代码行。
        values.put("passwordHash", row.getPasswordHash());
        // 注释：执行当前代码行。
        values.put("passwordAlgo", row.getPasswordAlgo());
        // 注释：执行当前代码行。
        values.put("passwordUpdateTime", row.getPasswordUpdateTime());
        // 注释：执行当前代码行。
        values.put("gender", row.getGender());
        // 注释：执行当前代码行。
        values.put("userType", row.getUserType());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("lastLoginAt", row.getLastLoginAt());
        // 注释：执行当前代码行。
        values.put("lastLoginIp", row.getLastLoginIp());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：执行当前代码行。
        values.put("updateTime", row.getUpdateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord recordPreference(SysUserPreferenceDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：判断当前业务条件。
        if (row == null) {
            // 注释：返回当前处理结果。
            return DomainRecord.of(values);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("userId", row.getUserId());
        // 注释：执行当前代码行。
        values.put("preferenceKey", row.getPreferenceKey());
        // 注释：执行当前代码行。
        values.put("preferenceValue", row.getPreferenceValue());
        // 注释：执行当前代码行。
        values.put("valueType", row.getValueType());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：执行当前代码行。
        values.put("updateTime", row.getUpdateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private <T> Optional<T> first(List<T> rows) {
        // 注释：返回当前处理结果。
        return rows == null || rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Long longValue(Object value) {
        // 注释：判断当前业务条件。
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return Long.parseLong(String.valueOf(value).trim());
            // 注释：捕获并处理当前异常。
        } catch (NumberFormatException ignored) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
