package top.kx.heartbeat.infrastructure.flow.flowable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

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
     * @return 投影摘要
     */
    public Map<String, Object> publishRawEvent(String eventType, String processInstanceId) {
        // 创建投影摘要。
        Map<String, Object> projection = new LinkedHashMap<>();
        // 写入投影模式。
        projection.put("projectionMode", projectionMode);
        // 写入事件类型。
        projection.put("eventType", eventType);
        // 写入流程实例标识。
        projection.put("processInstanceId", processInstanceId);
        // 返回投影摘要。
        return projection;
    }
}
