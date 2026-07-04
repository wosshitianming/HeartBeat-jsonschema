package top.kx.heartbeat.domain.common;

import java.util.List;

/**
 * 领域事件发布器（端口）。
 *
 * <p>领域/应用层只依赖此抽象，具体如何发布（Spring 事件、MQ、Outbox 表）由基础设施层实现，
 * 体现端口-适配器（六边形）思想。
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);

    default void publishAll(List<? extends DomainEvent> events) {
        if (events != null) {
            events.forEach(this::publish);
        }
    }
}
