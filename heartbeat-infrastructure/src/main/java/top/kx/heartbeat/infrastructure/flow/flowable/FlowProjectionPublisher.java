package top.kx.heartbeat.infrastructure.flow.flowable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Flow 运行投影发布器。
 *
 * <p>用于集中封装 LOCAL_TX、ASYNC_OUTBOX 和未来 MQ_PROJECTION 投影模式。</p>
 */
@Service
public class FlowProjectionPublisher {

    /**
     * 投影模式。
     */
    @Value("${heartbeat.flow.projection.mode:LOCAL_TX}")
    private String projectionMode;

    /**
     * 发布 Flowable 原始运行事件。
     *
     * @param eventType 事件类型
     * @param processInstanceId 流程实例标识
     */
    void publishRawEvent(String eventType, String processInstanceId) {
        // LOCAL_TX 模式下事件已在当前事务中完成；后续 MQ/Outbox 模式在这里扩展。
    }
}
