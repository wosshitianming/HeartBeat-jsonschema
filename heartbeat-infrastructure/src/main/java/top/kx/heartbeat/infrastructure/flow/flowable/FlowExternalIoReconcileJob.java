package top.kx.heartbeat.infrastructure.flow.flowable;

import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.common.response.RecordResponse;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 外部 I/O 对账任务。
 *
 * <p>用于处理 CALL_STARTED 后 worker 崩溃造成的下游状态不明场景。</p>
 */
@Component
public class FlowExternalIoReconcileJob {

    /**
     * 执行一次对账扫描。
     *
     * @return 对账扫描摘要
     */
    public RecordResponse reconcileOnce() {
        // 创建对账摘要。
        Map<String, Object> summary = new LinkedHashMap<>();
        // 写入执行时间。
        summary.put("executedAt", Instant.now());
        // 写入当前实现阶段。
        summary.put("stage", "PORT_READY");
        // 写入说明。
        summary.put("message", "外部 I/O 对账端口已就绪，命令表落库后接入扫描逻辑");
        // 返回对账摘要。
        return RecordResponse.from(summary);
    }
}
