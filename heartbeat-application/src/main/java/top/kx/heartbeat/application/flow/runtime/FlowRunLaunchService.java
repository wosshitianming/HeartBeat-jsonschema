package top.kx.heartbeat.application.flow.runtime;

import org.springframework.stereotype.Service;
import top.kx.heartbeat.domain.flow.model.FlowIdempotencyScope;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunStatus;
import top.kx.heartbeat.domain.flow.model.FlowRuntimeEngine;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;

import javax.annotation.Resource;
import java.time.Instant;

@Service
public class FlowRunLaunchService {
    @Resource
    private FlowRuntimeFacade flowRuntimeFacade;
    @Resource
    private FlowRunRepository flowRunRepository;
    @Resource
    private FlowRunLedgerService ledgerService;
    @Resource
    private FlowPayloadSummaryService payloadSummaryService;

    public FlowRun createPending(FlowStartCommand command) {
        FlowRun run = new FlowRun();
        run.setId(command.getRunId());
        run.setFlowId(command.getFlowId());
        run.setVersionNo(command.getVersionNo() == null ? 0 : command.getVersionNo());
        run.setFlowVersionId(command.getFlowVersionId());
        run.setRunNo("RUN-" + command.getRunId());
        run.setEngine(flowRuntimeFacade.productionEngine().getCode());
        run.setProcessDefinitionId(command.getProcessDefinitionId());
        run.setTriggerType(command.getTriggerType().getCode());
        run.setIdempotencyKey(command.getIdempotencyKey());
        run.setIdempotencyScope(FlowIdempotencyScope.START.getCode());
        run.setBusinessKey(command.getBusinessKey());
        run.setCorrelationKey(command.getCorrelationKey());
        run.setTenantId(command.getTenantId());
        run.setStatus(FlowRunStatus.CREATED.getCode());
        run.setInputSummary(payloadSummaryService.summarize(command.getPayload()));
        run.setStartedAt(Instant.now());
        return run;
    }

    public FlowRun start(FlowStartCommand command, FlowRun pending) {
        boolean persistent = !FlowRuntimeEngine.LOCAL_DEBUG.equals(flowRuntimeFacade.productionEngine());
        if (persistent) ledgerService.create(pending);
        try {
            FlowRun started = flowRuntimeFacade.start(command);
            copyLineage(pending, started);
            if (persistent) {
                FlowRun projected = flowRunRepository.findRun(command.getRunId()).orElse(pending);
                if (!FlowRunStatus.CREATED.matches(projected.getStatus())) return projected;
            }
            return flowRunRepository.saveRun(started);
        } catch (RuntimeException ex) {
            if (persistent) ledgerService.markStartFailed(pending, ex);
            throw ex;
        }
    }

    private void copyLineage(FlowRun source, FlowRun target) {
        target.setRunNo(source.getRunNo());
        target.setIdempotencyScope(source.getIdempotencyScope());
        target.setRetryFromRunId(source.getRetryFromRunId());
        target.setParentRunId(source.getParentRunId());
        target.setRootRunId(source.getRootRunId());
        target.setRetryNo(source.getRetryNo());
        target.setRetryReason(source.getRetryReason());
    }
}
