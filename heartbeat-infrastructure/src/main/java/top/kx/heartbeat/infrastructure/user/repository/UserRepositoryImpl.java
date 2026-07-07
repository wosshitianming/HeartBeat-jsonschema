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
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long tenantId = tenantId();
        // 返回已经完成封装的业务结果。
        return TenantContext.runAsPlatform(() -> {
            // 调用 Mapper 完成数据库读写，保持持久化状态与业务动作一致。
            SysUserDO entity = user.getId() == null ? null : selectById(tenantId, user.getId().value());
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (entity == null) {
                // 创建数据库记录对象，承载即将写入的业务字段。
                entity = new SysUserDO();
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setTenantId(tenantId);
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setUsername(user.getUsername());
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setNickname(user.getUsername());
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setPasswordHash(passwordEncoder.encode("disabled-" + System.nanoTime()));
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setPasswordAlgo("BCRYPT");
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setPasswordUpdateTime(new Date());
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setUserType("NORMAL");
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setVersion(0);
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setDeleteMarker(0L);
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                entity.setCreateTime(user.getCreateTime());
            }
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setEmail(user.getEmail().value());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setStatus(toPersistenceStatus(user.getStatus()));
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setUpdateTime(user.getUpdateTime());
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (entity.getId() == null) {
                // 将当前业务变更写入持久化层，保持数据状态同步。
                userMapper.insertSelective(entity);
            } else {
                // 将当前业务变更写入持久化层，保持数据状态同步。
                userMapper.updateByPrimaryKeySelective(entity);
            }
            // 返回已经完成封装的业务结果。
            return convert.toDomain(entity);
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        });
    }

    @Override
    public Optional<User> findById(UserId id) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long tenantId = tenantId();
        // 读取当前租户上下文，确保数据访问始终受租户隔离约束。
        SysUserDO entity = TenantContext.runAsPlatform(() -> selectById(tenantId, id.value()));
        // 返回已经完成封装的业务结果。
        return Optional.ofNullable(entity).map(convert::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long tenantId = tenantId();
        // 读取当前租户上下文，确保数据访问始终受租户隔离约束。
        SysUserDO entity = TenantContext.runAsPlatform(() -> selectByEmail(tenantId, email.value()));
        // 返回已经完成封装的业务结果。
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
