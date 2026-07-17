package top.kx.heartbeat.domain.flow.repository;

import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;
import top.kx.heartbeat.domain.flow.model.FlowRunQuery;
import top.kx.heartbeat.domain.flow.model.FlowRunStatistics;

import java.util.List;
import java.util.Optional;

/**
 * 流程运行实例领域仓储接口
 *
 * @author heartbeat-team
 */
public interface FlowRunRepository {

    /**
     * 保存一次运行实例
     */
    FlowRun saveRun(FlowRun run);

    /**
     * 记录一次运行事件
     */
    FlowRunEvent saveEvent(FlowRunEvent event);

    /**
     * 按主键查询运行实例
     */
    Optional<FlowRun> findRun(String runId);

    /**
     * 按主键加锁查询运行实例，用于串行化引擎事件投影。
     */
    Optional<FlowRun> findRunForUpdate(String runId);

    /**
     * 列出某流程下的全部运行实例
     */
    List<FlowRun> findRunsByFlowId(String flowId);

    Page<FlowRun> pageByQuery(FlowRunQuery query);

    FlowRunStatistics summarize(String flowId, java.time.Instant startedAfter, java.time.Instant startedBefore);

    Optional<FlowRun> findRunByEngineInstanceId(String engineInstanceId);

    /**
     * 列出某次运行的事件序列
     */
    List<FlowRunEvent> findEvents(String runId);

    class Page<T> {
        private final List<T> records;
        private final long total;
        private final int pageNum;
        private final int pageSize;

        public Page(List<T> records, long total, int pageNum, int pageSize) {
            this.records = records;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public List<T> getRecords() {
            return records;
        }

        public long getTotal() {
            return total;
        }

        public int getPageNum() {
            return pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }
    }
}
