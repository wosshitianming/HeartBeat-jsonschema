package top.kx.heartbeat.infrastructure.common.event;


import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.common.DomainEvent;
import top.kx.heartbeat.domain.common.DomainEventPublisher;

import javax.annotation.Resource;

/**
 * 基于 Spring 事件机制的领域事件发布器实现（端口的适配器）。
 *
 * <p>当前进程内同步派发，便于演示；后续可无缝替换为基于 Outbox + MQ 的可靠异步实现，
 * 而上层代码无需改动。
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
