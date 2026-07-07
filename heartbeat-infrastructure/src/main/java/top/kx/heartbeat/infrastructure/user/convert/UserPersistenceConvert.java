package top.kx.heartbeat.infrastructure.user.convert;

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
public abstract class UserPersistenceConvert {

    @BeanMapping(ignoreByDefault = true)
    public abstract User toDomain(SysUserDO entity);

    @ObjectFactory
    protected User rehydrate(SysUserDO entity) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        UserStatus status = PlatformRecordStatus.DISABLED.getCode().equals(entity.getStatus())
                // 条件成立时使用前一个分支计算出的业务值。
                ? UserStatus.DISABLED
                // 条件不成立时使用兜底业务值。
                : UserStatus.ACTIVE;
        // 返回已经完成封装的业务结果。
        return User.rehydrate(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                UserId.of(entity.getId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getUsername(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                Email.of(entity.getEmail()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                status,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getCreateTime(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getUpdateTime()
        );
    }
}
