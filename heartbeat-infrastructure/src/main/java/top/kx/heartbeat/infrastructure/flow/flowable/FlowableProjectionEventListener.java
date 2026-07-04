package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Flowable 全局事件投影监听器。
 *
 * <p>用于监听 Flowable 运行事件并发布为 HeartBeat 运行记录投影。</p>
 */
@Component
public class FlowableProjectionEventListener implements FlowableEventListener {

    /**
     * Flow 投影发布器。
     */
    @Resource
    private FlowProjectionPublisher projectionPublisher;

    /**
     * 处理 Flowable 事件。
     *
     * @param event Flowable 事件
     */
    @Override
    public void onEvent(FlowableEvent event) {
        // 发布 Flowable 原始事件摘要。
        projectionPublisher.publishRawEvent(String.valueOf(event.getType()), "");
    }

    /**
     * 判断监听器异常是否回滚 Flowable 事务。
     *
     * @return 是否异常回滚
     */
    @Override
    public boolean isFailOnException() {
        // 投影失败必须回滚 Flowable 推进事务。
        return true;
    }

    /**
     * 判断是否绑定事务生命周期事件。
     *
     * @return 是否绑定事务生命周期
     */
    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        // 当前监听器直接在 Flowable 事件阶段执行。
        return false;
    }

    /**
     * 获取事务生命周期阶段。
     *
     * @return 事务生命周期阶段
     */
    @Override
    public String getOnTransaction() {
        // 不绑定具体事务生命周期阶段。
        return null;
    }
}
