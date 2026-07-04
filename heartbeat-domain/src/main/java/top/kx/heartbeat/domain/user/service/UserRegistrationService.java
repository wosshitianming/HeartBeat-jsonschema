package top.kx.heartbeat.domain.user.service;

import top.kx.heartbeat.domain.common.exception.DomainException;
import top.kx.heartbeat.domain.user.UserErrorCode;
import top.kx.heartbeat.domain.user.model.User;
import top.kx.heartbeat.domain.user.model.valueobject.Email;
import top.kx.heartbeat.domain.user.repository.UserRepository;

import javax.annotation.Resource;

/**
 * 用户注册领域服务。
 *
 * <p>邮箱全局唯一性是一条跨越单个聚合实例的规则——它无法只靠某个 {@code User} 实例自身判断，
 * 因此适合放在领域服务中，借助仓储查询其他聚合的状态。这类"无法自然归属于某个实体/值对象"的
 * 领域逻辑，正是领域服务的职责。
 *
 * <p>本类是纯领域对象，不使用任何框架注解，由应用层负责实例化与注入。
 */
public class UserRegistrationService {

    @Resource
    private UserRepository userRepository;

    /**
     * 在保证邮箱唯一的前提下创建新用户聚合（尚未持久化）。
     */
    public User register(String username, Email email) {
        if (userRepository.existsByEmail(email)) {
            throw new DomainException(UserErrorCode.EMAIL_DUPLICATED, "邮箱已被注册: " + email.value());
        }
        return User.register(username, email);
    }
}
