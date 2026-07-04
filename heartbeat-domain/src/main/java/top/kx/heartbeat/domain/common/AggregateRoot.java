package top.kx.heartbeat.domain.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聚合根抽象基类。
 *
 * <p>聚合根是聚合的唯一入口，负责维护聚合内部的业务不变量（invariant），并对外屏蔽内部实体。
 * 仓储（Repository）只针对聚合根，外部不得绕过聚合根直接持久化内部实体。
 *
 * <p>聚合根在状态变更时登记领域事件（{@link #registerEvent}），由应用层在事务边界内取出并发布。
 *
 * @param <ID> 聚合根标识类型
 */
public abstract class AggregateRoot<ID> {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 聚合根的唯一标识。
     */
    public abstract ID getId();

    /**
     * 登记一条领域事件，等待应用层在事务提交时统一发布。
     */
    protected void registerEvent(DomainEvent event) {
        if (event != null) {
            this.domainEvents.add(event);
        }
    }

    /**
     * 取出已登记的领域事件（只读视图）。
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * 清空已登记的领域事件，通常在事件发布完成后调用。
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
