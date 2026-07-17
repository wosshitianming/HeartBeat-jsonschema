package top.kx.heartbeat.application.flow.runtime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;
import top.kx.heartbeat.domain.flow.model.FlowRunStatus;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;

@Service
public class FlowRunLedgerService {
    @Resource
    private FlowRunRepository flowRunRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FlowRun create(FlowRun pending) {
        return flowRunRepository.saveRun(pending);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FlowRun markStartFailed(FlowRun pending, RuntimeException error) {
        FlowRun run = flowRunRepository.findRunForUpdate(pending.getId()).orElse(pending);
        if (FlowRunStatus.isTerminal(run.getStatus())) return run;
        Instant finishedAt = Instant.now();
        run.setStatus(FlowRunStatus.FAILED.getCode());
        run.setErrorMessage(error == null ? "流程启动失败" : error.getMessage());
        run.setFinishedAt(finishedAt);
        if (run.getStartedAt() != null) {
            run.setElapsedMs(Duration.between(run.getStartedAt(), finishedAt).toMillis());
        }
        FlowRun saved = flowRunRepository.saveRun(run);
        FlowRunEvent event = new FlowRunEvent();
        event.setRunId(saved.getId());
        event.setNodeId("__process__");
        event.setNodeType("PROCESS");
        event.setEventType("RUN_FAILED");
        event.setErrorMessage(saved.getErrorMessage());
        event.setCreateTime(finishedAt);
        flowRunRepository.saveEvent(event);
        return saved;
    }
}
