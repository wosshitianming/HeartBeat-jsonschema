package top.kx.heartbeat.domain.user.repository;

import top.kx.heartbeat.domain.user.model.User;
import top.kx.heartbeat.domain.user.model.valueobject.Email;
import top.kx.heartbeat.domain.user.model.valueobject.UserId;

import java.util.Optional;

/**
 * 用户仓储接口（领域层定义契约，基础设施层提供实现 —— 依赖倒置）。
 *
 * <p>仓储以聚合根为单位进行存取，向领域屏蔽具体持久化技术（JPA/MyBatis/远程服务皆可）。
 * 返回与入参一律使用领域模型，而非持久化对象（PO）。
 */
public interface UserRepository {

    /**
     * 生成下一个用户标识（ID 生成策略属于基础设施关注点）。
     */
    /**
     * 保存（新增或更新）聚合根。
     */
    User save(User user);

    /**
     * 按标识查找聚合根。
     */
    Optional<User> findById(UserId id);

    /**
     * 按邮箱查找聚合根。
     */
    Optional<User> findByEmail(Email email);

    /**
     * 判断邮箱是否已存在，用于注册时的唯一性校验。
     */
    boolean existsByEmail(Email email);
}
