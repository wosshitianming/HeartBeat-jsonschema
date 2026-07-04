package top.kx.heartbeat.domain.common;

import java.time.Instant;

/**
 * 领域事件标记接口。
 *
 * <p>领域事件表达"领域中已经发生的、对业务有意义的事实"，命名一律使用过去式（如 UserRegistered）。
 * 事件由聚合根在状态变更时登记，由应用层在事务提交后统一发布。
 */
public interface DomainEvent {

    /**
     * 事件发生时间，用于排序、审计与幂等。
     */
    Instant occurredOn();
}
