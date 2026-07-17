package top.kx.heartbeat.infrastructure.flow.flowable;

import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.common.response.RecordResponse;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 外部 I/O 对账任务。
 *
 * <p>用于处理 CALL_STARTED 后 worker 崩溃造成的下游状态不明场景。</p>
 */
@Component("flowExternalIoReconcileJob")
public class FlowExternalIoReconcileJob {

    @Resource
    private FlowExternalIoCommandDispatcher commandDispatcher;

    /**
     * 执行一次对账扫描。
     *
     * @return 对账扫描摘要
     */
    public RecordResponse reconcileOnce() {
        Map<String, Object> summary = new LinkedHashMap<>(commandDispatcher.reconcileExpiredLeases(200));
        // 写入执行时间。
        summary.put("executedAt", Instant.now());
        summary.put("stage", "LEASE_RECONCILED");
        // 返回对账摘要。
        return RecordResponse.from(summary);
    }
}
