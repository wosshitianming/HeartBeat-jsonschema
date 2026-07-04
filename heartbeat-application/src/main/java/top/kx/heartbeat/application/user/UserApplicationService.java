package top.kx.heartbeat.application.user;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.user.assembler.UserAssembler;
import top.kx.heartbeat.application.user.command.ChangeEmailCommand;
import top.kx.heartbeat.application.user.command.RegisterUserCommand;
import top.kx.heartbeat.application.user.dto.UserDTO;
import top.kx.heartbeat.domain.common.DomainEventPublisher;
import top.kx.heartbeat.domain.common.exception.DomainException;
import top.kx.heartbeat.domain.user.UserErrorCode;
import top.kx.heartbeat.domain.user.event.UserRegisteredEvent;
import top.kx.heartbeat.domain.user.model.User;
import top.kx.heartbeat.domain.user.model.valueobject.Email;
import top.kx.heartbeat.domain.user.model.valueobject.UserId;
import top.kx.heartbeat.domain.user.repository.UserRepository;
import top.kx.heartbeat.domain.user.service.UserRegistrationService;

import javax.annotation.Resource;

/**
 * 用户应用服务（用例编排）。
 *
 * <p>应用层只做"编排"，不写业务规则：它负责管理事务边界、调用领域对象/领域服务完成业务、
 * 持久化聚合、发布领域事件，并把领域模型转换为 DTO 返回。业务规则一律下沉到领域层。
 */
@Slf4j
@Service
public class UserApplicationService {

    @Resource
    private UserRepository userRepository;
    @Resource
    private UserRegistrationService userRegistrationService;
    @Resource
    private DomainEventPublisher domainEventPublisher;
    @Resource
    private UserAssembler userAssembler;

    /**
     * 用例：注册用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDTO register(RegisterUserCommand command) {
        Email email = Email.of(command.getEmail());
        User user = userRegistrationService.register(command.getUsername(), email);

        User saved = userRepository.save(user);
        domainEventPublisher.publish(new UserRegisteredEvent(saved.getId(), email));

        log.info("用户注册成功, userId={}, email={}", saved.getId(), email.value());
        return userAssembler.toDTO(saved);
    }

    /**
     * 用例：修改邮箱。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDTO changeEmail(ChangeEmailCommand command) {
        User user = loadRequired(UserId.of(command.getUserId()));
        user.changeEmail(Email.of(command.getNewEmail()));

        User saved = userRepository.save(user);
        log.info("用户邮箱已修改, userId={}", saved.getId());
        return userAssembler.toDTO(saved);
    }

    /**
     * 用例：停用用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long userId) {
        User user = loadRequired(UserId.of(userId));
        user.disable();
        userRepository.save(user);
        log.info("用户已停用, userId={}", userId);
    }

    /**
     * 查询：按 ID 获取用户详情。
     */
    public UserDTO getById(Long userId) {
        return userAssembler.toDTO(loadRequired(UserId.of(userId)));
    }

    private User loadRequired(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND,
                        "用户不存在: " + userId.value()));
    }
}
