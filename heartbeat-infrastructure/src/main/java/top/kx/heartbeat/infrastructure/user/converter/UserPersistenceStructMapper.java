package top.kx.heartbeat.infrastructure.user.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import top.kx.heartbeat.domain.platform.PlatformRecordStatus;
import top.kx.heartbeat.domain.user.model.User;
import top.kx.heartbeat.domain.user.model.valueobject.Email;
import top.kx.heartbeat.domain.user.model.valueobject.UserId;
import top.kx.heartbeat.domain.user.model.valueobject.UserStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDO;

@Mapper(componentModel = "spring")
public abstract class UserPersistenceStructMapper {

    @BeanMapping(ignoreByDefault = true)
    public abstract User toDomain(SysUserDO entity);

    @ObjectFactory
    protected User rehydrate(SysUserDO entity) {
        UserStatus status = PlatformRecordStatus.DISABLED.getCode().equals(entity.getStatus())
                ? UserStatus.DISABLED
                : UserStatus.ACTIVE;
        return User.rehydrate(
                UserId.of(entity.getId()),
                entity.getUsername(),
                Email.of(entity.getEmail()),
                status,
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }
}
