package top.kx.heartbeat.infrastructure.flow.flowable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.delegate.event.*;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableProcessEngineEvent;
import org.flowable.engine.delegate.event.FlowableSequenceFlowTakenEvent;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;
import top.kx.heartbeat.domain.flow.model.FlowRunStatus;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FlowProjectionPublisher {
    @Resource
    private FlowRunRepository flowRunRepository;
    @Resource
    private FlowableVariableCodec variableCodec;
    @Resource
    private FlowableExceptionTranslator exceptionTranslator;
    @Lazy
    @Resource
    private RuntimeService runtimeService;

    @Transactional
    public void publish(FlowableEvent event) {
        if (!(event instanceof FlowableEngineEvent)) return;
        FlowableEngineEvent engineEvent = (FlowableEngineEvent) event;
        ProjectionContext context = resolveContext(event, engineEvent);
        if (context == null) return;
        TenantContext.runAsTenant(context.tenantId, () -> {
            project(event, engineEvent, context);
            return null;
        });
    }

    private void project(FlowableEvent raw, FlowableEngineEvent engine, ProjectionContext context) {
        Optional<FlowRun> locked = lockedRun(context.runId, engine.getProcessInstanceId());
        if (!locked.isPresent()) return;
        FlowRun run = locked.get();
        if (FlowRunStatus.isTerminal(run.getStatus())) return;
        FlowableEngineEventType type = (FlowableEngineEventType) raw.getType();
        switch (type) {
            case PROCESS_STARTED:
                run.setInputSummary(payloadForProjection(context.execution));
                markRunning(run, engine);
                appendProcessEvent(run, engine, context, "PROCESS_STARTED", null, null);
                break;
            case ACTIVITY_STARTED:
                markRunning(run, engine);
                appendActivityEvent(run, engine, context, (FlowableActivityEvent) raw, "ACTIVITY_STARTED", false);
                break;
            case ACTIVITY_COMPLETED:
                appendActivityEvent(run, engine, context, (FlowableActivityEvent) raw, "ACTIVITY_COMPLETED", true);
                break;
            case ACTIVITY_CANCELLED:
                appendActivityEvent(run, engine, context, (FlowableActivityEvent) raw, "ACTIVITY_CANCELLED", true);
                break;
            case ACTIVITY_MESSAGE_WAITING:
            case ACTIVITY_SIGNAL_WAITING:
            case ACTIVITY_CONDITIONAL_WAITING:
                markWaiting(run);
                appendActivityEvent(run, engine, context, (FlowableActivityEvent) raw, "ACTIVITY_WAITING", false);
                break;
            case ACTIVITY_MESSAGE_RECEIVED:
            case ACTIVITY_SIGNALED:
            case ACTIVITY_CONDITIONAL_RECEIVED:
                markRunning(run, engine);
                appendActivityEvent(run, engine, context, (FlowableActivityEvent) raw, "ACTIVITY_RESUMED", false);
                break;
            case TASK_CREATED:
                markWaiting(run);
                appendTaskEvent(run, engine, context, raw, "TASK_CREATED");
                break;
            case TASK_COMPLETED:
                markRunning(run, engine);
                appendTaskEvent(run, engine, context, raw, "TASK_COMPLETED");
                break;
            case TIMER_SCHEDULED:
                markWaiting(run);
                appendJobEvent(run, engine, context, raw, "TIMER_SCHEDULED");
                break;
            case TIMER_FIRED:
                markRunning(run, engine);
                appendJobEvent(run, engine, context, raw, "TIMER_FIRED");
                break;
            case SEQUENCEFLOW_TAKEN:
                appendSequenceEvent(run, engine, context, (FlowableSequenceFlowTakenEvent) raw);
                break;
            case PROCESS_COMPLETED:
            case PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT:
                finishCompletedProcess(run, engine, context);
                break;
            case PROCESS_COMPLETED_WITH_ERROR_END_EVENT:
                finish(run, engine, context, FlowRunStatus.FAILED,
                        text(context.variables.get("hbLastErrorCode")), text(context.variables.get("hbLastErrorMessage")));
                break;
            case PROCESS_CANCELLED:
                finish(run, engine, context, FlowRunStatus.CANCELED, null,
                        raw instanceof FlowableCancelledEvent ? text(((FlowableCancelledEvent) raw).getCause()) : "流程已取消");
                break;
            case JOB_EXECUTION_FAILURE:
                appendJobEvent(run, engine, context, raw, "JOB_EXECUTION_FAILURE");
                break;
            case JOB_MOVED_TO_DEADLETTER:
                String message = errorMessage(raw, job(raw), "异步任务进入死信队列");
                appendJobEvent(run, engine, context, raw, "JOB_MOVED_TO_DEADLETTER");
                finish(run, engine, context, FlowRunStatus.FAILED,
                        exceptionTranslator.translateCode(new IllegalStateException(message)), message);
                break;
            default:
                break;
        }
    }

    private void markRunning(FlowRun run, FlowableEngineEvent event) {
        if (FlowRunStatus.isTerminal(run.getStatus())) return;
        run.setStatus(FlowRunStatus.RUNNING.getCode());
        run.setEngineInstanceId(event.getProcessInstanceId());
        run.setProcessDefinitionId(event.getProcessDefinitionId());
        if (run.getStartedAt() == null) run.setStartedAt(Instant.now());
        flowRunRepository.saveRun(run);
    }

    private void markWaiting(FlowRun run) {
        if (FlowRunStatus.isTerminal(run.getStatus())) return;
        run.setStatus(FlowRunStatus.WAITING.getCode());
        flowRunRepository.saveRun(run);
    }

    private void finish(FlowRun run, FlowableEngineEvent engine, ProjectionContext context,
                        FlowRunStatus status, String errorCode, String errorMessage) {
        if (FlowRunStatus.isTerminal(run.getStatus())) return;
        Instant finishedAt = Instant.now();
        run.setStatus(status.getCode());
        run.setEngineInstanceId(engine.getProcessInstanceId());
        run.setProcessDefinitionId(engine.getProcessDefinitionId());
        run.setOutputSummary(payloadForProjection(context.execution));
        run.setErrorMessage(errorMessage);
        run.setFinishedAt(finishedAt);
        if (run.getStartedAt() != null) run.setElapsedMs(Duration.between(run.getStartedAt(), finishedAt).toMillis());
        flowRunRepository.saveRun(run);
        appendProcessEvent(run, engine, context, "PROCESS_" + status.getCode(), errorCode, errorMessage);
    }

    private void finishCompletedProcess(FlowRun run, FlowableEngineEvent engine, ProjectionContext context) {
        boolean failed = FlowRunStatus.FAILED.getCode().equals(
                text(context.variables.get("hbLastNodeStatus")));
        finish(run, engine, context, failed ? FlowRunStatus.FAILED : FlowRunStatus.SUCCESS,
                failed ? text(context.variables.get("hbLastErrorCode")) : null,
                failed ? text(context.variables.get("hbLastErrorMessage")) : null);
    }

    private void appendProcessEvent(FlowRun run, FlowableEngineEvent engine, ProjectionContext context,
                                    String eventType, String errorCode, String errorMessage) {
        FlowRunEvent event = baseEvent(run, engine, context, "__process__", "PROCESS", eventType);
        applyOutputPayload(event, context.execution);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);
        flowRunRepository.saveEvent(event);
    }

    private void appendActivityEvent(FlowRun run, FlowableEngineEvent engine, ProjectionContext context,
                                     FlowableActivityEvent activity, String eventType, boolean includeOutput) {
        FlowRunEvent event = baseEvent(run, engine, context, normalizeNodeId(activity.getActivityId()),
                StringUtils.defaultIfBlank(activity.getActivityType(), "ACTIVITY"), eventType);
        event.setEngineActivityId(activity.getActivityId());
        applyInputPayload(event, context.execution);
        if (includeOutput) {
            applyOutputPayload(event, context.execution);
            Map<String, Object> selected = new LinkedHashMap<>();
            selected.put("ports", context.execution == null ? Collections.emptyList() : variableCodec.readNextPorts(context.execution));
            event.setSelectedPorts(selected);
        }
        event.setErrorCode(text(context.variables.get("hbLastErrorCode")));
        event.setErrorMessage(text(context.variables.get("hbLastErrorMessage")));
        flowRunRepository.saveEvent(event);
    }

    private void appendSequenceEvent(FlowRun run, FlowableEngineEvent engine, ProjectionContext context,
                                     FlowableSequenceFlowTakenEvent sequence) {
        FlowRunEvent event = baseEvent(run, engine, context, "__sequence__", "SEQUENCE_FLOW", "SEQUENCEFLOW_TAKEN");
        event.setEdgeId(sequence.getId());
        event.setSourceNodeId(normalizeNodeId(sequence.getSourceActivityId()));
        event.setTargetNodeId(normalizeNodeId(sequence.getTargetActivityId()));
        flowRunRepository.saveEvent(event);
    }

    private void appendTaskEvent(FlowRun run, FlowableEngineEvent engine, ProjectionContext context,
                                 FlowableEvent raw, String eventType) {
        Task task = task(raw);
        FlowRunEvent event = baseEvent(run, engine, context,
                task == null ? "__task__" : normalizeNodeId(task.getTaskDefinitionKey()), "USER_TASK", eventType);
        if (task != null) {
            event.setTaskId(task.getId());
            event.getEventSummary().put("assignee", task.getAssignee());
            event.getEventSummary().put("owner", task.getOwner());
        }
        flowRunRepository.saveEvent(event);
    }

    private void appendJobEvent(FlowRun run, FlowableEngineEvent engine, ProjectionContext context,
                                FlowableEvent raw, String eventType) {
        flowRunRepository.saveEvent(jobEvent(run, engine, context, raw, eventType));
    }

    private FlowRunEvent jobEvent(FlowRun run, FlowableEngineEvent engine, ProjectionContext context,
                                  FlowableEvent raw, String eventType) {
        Job job = job(raw);
        FlowRunEvent event = baseEvent(run, engine, context,
                job == null ? "__job__" : normalizeNodeId(job.getElementId()), "ASYNC_JOB", eventType);
        Throwable cause = raw instanceof FlowableExceptionEvent ? ((FlowableExceptionEvent) raw).getCause() : null;
        if (job != null) {
            event.getEventSummary().put("jobId", job.getId());
            event.getEventSummary().put("retriesBeforeFailure", job.getRetries());
        }
        event.setErrorCode(cause == null ? null : exceptionTranslator.translateCode(cause));
        event.setErrorMessage(errorMessage(raw, job, null));
        return event;
    }

    private FlowRunEvent baseEvent(FlowRun run, FlowableEngineEvent engine, ProjectionContext context,
                                   String nodeId, String nodeType, String eventType) {
        FlowRunEvent event = new FlowRunEvent();
        event.setRunId(run.getId());
        event.setExecutionId(engine.getExecutionId());
        event.setNodeId(StringUtils.defaultIfBlank(nodeId, "__process__"));
        event.setNodeType(StringUtils.defaultIfBlank(nodeType, "UNKNOWN"));
        event.setEventType(eventType);
        event.setAttemptNo(1);
        event.setCreateTime(Instant.now());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("processInstanceId", engine.getProcessInstanceId());
        summary.put("processDefinitionId", engine.getProcessDefinitionId());
        summary.put("tenantId", context.tenantId);
        event.setEventSummary(summary);
        return event;
    }

    private ProjectionContext resolveContext(FlowableEvent raw, FlowableEngineEvent engine) {
        DelegateExecution execution = raw instanceof FlowableProcessEngineEvent
                ? ((FlowableProcessEngineEvent) raw).getExecution() : null;
        Map<String, Object> variables = execution == null ? variables(engine.getProcessInstanceId())
                : new LinkedHashMap<>(execution.getVariables());
        Job job = job(raw);
        Task task = task(raw);
        String variableTenantId = text(variables.get(FlowableVariableCodec.TENANT_ID));
        String nativeTenantId = execution != null ? execution.getTenantId()
                : job != null ? job.getTenantId() : task == null ? null : task.getTenantId();
        if (StringUtils.isNotBlank(variableTenantId) && StringUtils.isNotBlank(nativeTenantId)
                && !variableTenantId.equals(nativeTenantId)) {
            throw new IllegalStateException("Flowable 事件租户与运行变量不一致");
        }
        String tenantId = StringUtils.defaultIfBlank(nativeTenantId, variableTenantId);
        String runId = text(variables.get(FlowableVariableCodec.RUN_ID));
        return StringUtils.isBlank(tenantId)
                ? null : new ProjectionContext(runId, tenantId, execution, variables);
    }

    private Map<String, Object> variables(String processInstanceId) {
        if (StringUtils.isBlank(processInstanceId)) return Collections.emptyMap();
        try {
            return new LinkedHashMap<>(runtimeService.getVariables(processInstanceId));
        } catch (RuntimeException ignored) {
            return Collections.emptyMap();
        }
    }

    private Optional<FlowRun> lockedRun(String runId, String engineInstanceId) {
        if (StringUtils.isNotBlank(engineInstanceId)) {
            Optional<FlowRun> byEngine = flowRunRepository.findRunByEngineInstanceId(engineInstanceId);
            if (byEngine.isPresent()) {
                FlowRun bound = byEngine.get();
                if (StringUtils.isNotBlank(runId) && !runId.equals(bound.getId())) {
                    throw new IllegalStateException("Flowable 实例与运行账本标识不一致");
                }
                return Optional.of(flowRunRepository.findRunForUpdate(bound.getId())
                        .orElseThrow(() -> new IllegalStateException("Flowable 运行账本不存在: " + bound.getId())));
            }
        }
        if (StringUtils.isBlank(runId)) {
            return Optional.empty();
        }
        return Optional.of(flowRunRepository.findRunForUpdate(runId)
                .orElseThrow(() -> new IllegalStateException("Flowable 运行账本不存在: " + runId)));
    }

    private Map<String, Object> payloadForProjection(DelegateExecution execution) {
        return execution == null
                ? new LinkedHashMap<>() : variableCodec.readPayloadForProjection(execution);
    }

    private void applyInputPayload(FlowRunEvent event, DelegateExecution execution) {
        event.setInput(payloadForProjection(execution));
        event.setInputPayloadRef(execution == null ? null : variableCodec.readPayloadReference(execution));
    }

    private void applyOutputPayload(FlowRunEvent event, DelegateExecution execution) {
        event.setOutput(payloadForProjection(execution));
        event.setOutputPayloadRef(execution == null ? null : variableCodec.readPayloadReference(execution));
    }

    private Job job(FlowableEvent event) {
        if (!(event instanceof FlowableEngineEntityEvent)) return null;
        Object entity = ((FlowableEngineEntityEvent) event).getEntity();
        return entity instanceof Job ? (Job) entity : null;
    }

    private Task task(FlowableEvent event) {
        if (!(event instanceof FlowableEngineEntityEvent)) return null;
        Object entity = ((FlowableEngineEntityEvent) event).getEntity();
        return entity instanceof Task ? (Task) entity : null;
    }

    private String errorMessage(FlowableEvent raw, Job job, String fallback) {
        Throwable cause = raw instanceof FlowableExceptionEvent ? ((FlowableExceptionEvent) raw).getCause() : null;
        if (cause != null && StringUtils.isNotBlank(cause.getMessage())) return cause.getMessage();
        return job == null ? fallback : StringUtils.defaultIfBlank(job.getExceptionMessage(), fallback);
    }

    private String normalizeNodeId(String value) {
        String nodeId = StringUtils.removeStart(StringUtils.defaultString(value), "node_");
        nodeId = StringUtils.substringBefore(nodeId, "__io_wait");
        nodeId = StringUtils.substringBefore(nodeId, "__io_result");
        return StringUtils.substringBefore(nodeId, "__io_failed");
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static final class ProjectionContext {
        private final String runId;
        private final String tenantId;
        private final DelegateExecution execution;
        private final Map<String, Object> variables;

        private ProjectionContext(String runId, String tenantId, DelegateExecution execution, Map<String, Object> variables) {
            this.runId = runId;
            this.tenantId = tenantId;
            this.execution = execution;
            this.variables = variables;
        }
    }
}
