package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformUserRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserPreferenceDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserPreferenceDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserPreferenceDOMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PlatformUserRepositoryImpl extends AbstractPlatformRepositorySupport implements PlatformUserRepository {

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
        return first(userPreferenceMapper.selectByExampleWithBLOBs(example)).map(this::record);
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
        return record(row);
    }

    @Override
    public List<DomainRecord> listUsers() {
        SysUserDOExample example = new SysUserDOExample();
        example.setOrderByClause("create_time DESC, id DESC");
        return records(userMapper.selectByExample(example));
    }

    @Override
    public DomainRecord createUser(Map<String, Object> command) {
        return create(userMapper, new SysUserDO(), command);
    }

    @Override
    public DomainRecord updateUser(String id, Map<String, Object> command) {
        return update(userMapper, new SysUserDO(), id, command);
    }

    @Override
    public void deleteUser(String id) {
        delete(userMapper, id);
    }

    @Override
    public DomainRecord createSocialUser(Map<String, Object> command) {
        return createUser(command);
    }
}
