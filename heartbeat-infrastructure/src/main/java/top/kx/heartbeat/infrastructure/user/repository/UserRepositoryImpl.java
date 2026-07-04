package top.kx.heartbeat.infrastructure.user.repository;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.platform.PlatformRecordStatus;
import top.kx.heartbeat.domain.user.model.User;
import top.kx.heartbeat.domain.user.model.valueobject.Email;
import top.kx.heartbeat.domain.user.model.valueobject.UserId;
import top.kx.heartbeat.domain.user.model.valueobject.UserStatus;
import top.kx.heartbeat.domain.user.repository.UserRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;
import top.kx.heartbeat.infrastructure.user.convert.UserPersistenceConvert;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

/**
 * 用户领域仓储统一落到正式 sys_user 表，主键由数据库自增生成。
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private static final long DEFAULT_TENANT_ID = 1L;

    @Resource
    private SysUserDOMapper userMapper;
    @Resource
    private UserPersistenceConvert convert;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public User save(User user) {
        Long tenantId = tenantId();
        return TenantContext.runAsPlatform(() -> {
            SysUserDO entity = user.getId() == null ? null : selectById(tenantId, user.getId().value());
            if (entity == null) {
                entity = new SysUserDO();
                entity.setTenantId(tenantId);
                entity.setUsername(user.getUsername());
                entity.setNickname(user.getUsername());
                entity.setPasswordHash(passwordEncoder.encode("disabled-" + System.nanoTime()));
                entity.setPasswordAlgo("BCRYPT");
                entity.setPasswordUpdateTime(new Date());
                entity.setUserType("NORMAL");
                entity.setVersion(0);
                entity.setDeleteMarker(0L);
                entity.setCreateTime(user.getCreateTime());
            }
            entity.setEmail(user.getEmail().value());
            entity.setStatus(toPersistenceStatus(user.getStatus()));
            entity.setUpdateTime(user.getUpdateTime());
            if (entity.getId() == null) {
                userMapper.insertSelective(entity);
            } else {
                userMapper.updateByPrimaryKeySelective(entity);
            }
            return convert.toDomain(entity);
        });
    }

    @Override
    public Optional<User> findById(UserId id) {
        Long tenantId = tenantId();
        SysUserDO entity = TenantContext.runAsPlatform(() -> selectById(tenantId, id.value()));
        return Optional.ofNullable(entity).map(convert::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        Long tenantId = tenantId();
        SysUserDO entity = TenantContext.runAsPlatform(() -> selectByEmail(tenantId, email.value()));
        return Optional.ofNullable(entity).map(convert::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        Long tenantId = tenantId();
        return TenantContext.runAsPlatform(() -> userMapper.countByExample(emailExample(tenantId, email.value()))) > 0;
    }

    private SysUserDO selectById(Long tenantId, Long id) {
        SysUserDOExample example = new SysUserDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andIdEqualTo(id)
                .andDeleteMarkerEqualTo(0L);
        return first(userMapper.selectByExample(example));
    }

    private SysUserDO selectByEmail(Long tenantId, String email) {
        return first(userMapper.selectByExample(emailExample(tenantId, email)));
    }

    private SysUserDOExample emailExample(Long tenantId, String email) {
        SysUserDOExample example = new SysUserDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andEmailEqualTo(email)
                .andDeleteMarkerEqualTo(0L);
        return example;
    }

    private SysUserDO first(java.util.List<SysUserDO> records) {
        return records.isEmpty() ? null : records.get(0);
    }

    private String toPersistenceStatus(UserStatus status) {
        // 领域用户状态与平台持久化状态在仓储层显式转换。
        return status == UserStatus.DISABLED ? PlatformRecordStatus.DISABLED.getCode() : PlatformRecordStatus.ENABLED.getCode();
    }

    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

}
