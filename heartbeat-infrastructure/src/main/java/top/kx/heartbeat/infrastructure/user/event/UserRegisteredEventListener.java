package top.kx.heartbeat.infrastructure.user.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.user.event.UserRegisteredEvent;

/**
 * 用户注册事件监听器（示例）。
 *
 * <p>演示领域事件的下游消费：真实场景可在此发送欢迎邮件、初始化用户画像等。
 * 这类副作用与主流程解耦，保证核心用例的单一职责。
 */
@Slf4j
@Component
public class UserRegisteredEventListener {

    @EventListener
    public void on(UserRegisteredEvent event) {
        log.info("[领域事件] 收到用户注册事件, userId={}, email={}, 触发时间={}",
                event.userId(), event.email().value(), event.occurredOn());
    }
}
