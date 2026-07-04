package top.kx.heartbeat.domain.flow.repository;

import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;

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
     * 列出某流程下的全部运行实例
     */
    List<FlowRun> findRunsByFlowId(String flowId);

    /**
     * 列出某次运行的事件序列
     */
    List<FlowRunEvent> findEvents(String runId);
}
