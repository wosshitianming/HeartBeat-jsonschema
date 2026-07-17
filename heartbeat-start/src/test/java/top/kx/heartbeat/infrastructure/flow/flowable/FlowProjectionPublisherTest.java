package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableExceptionEvent;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableProcessEngineEvent;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;
import top.kx.heartbeat.domain.flow.model.FlowRunStatus;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowProjectionPublisherTest {

    @Mock
    private FlowRunRepository flowRunRepository;
    @Mock
    private FlowableVariableCodec variableCodec;
    @Mock
    private FlowableExceptionTranslator exceptionTranslator;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private FlowableProcessEngineEvent event;
    @Mock
    private FlowableEngineEntityEvent jobEvent;
    @Mock
    private DelegateExecution execution;
    @Mock
    private Job job;
    @Mock
    private Task task;
    @InjectMocks
    private FlowProjectionPublisher publisher;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void completedProcessClosesLedgerAndAppendsReplayEvent() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(FlowableVariableCodec.RUN_ID, "101");
        variables.put(FlowableVariableCodec.TENANT_ID, "7");
        FlowRun run = new FlowRun();
        run.setId("101");
        run.setStatus(FlowRunStatus.RUNNING.getCode());
        run.setStartedAt(Instant.now().minusSeconds(2));
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("ok", true);
        when(event.getType()).thenReturn(FlowableEngineEventType.PROCESS_COMPLETED);
        when(event.getExecution()).thenReturn(execution);
        when(event.getProcessInstanceId()).thenReturn("pi-1");
        when(event.getProcessDefinitionId()).thenReturn("pd-1");
        when(event.getExecutionId()).thenReturn("ex-1");
        when(execution.getVariables()).thenReturn(variables);
        when(variableCodec.readPayloadForProjection(execution)).thenReturn(output);
        when(variableCodec.readPayloadReference(execution)).thenReturn("501");
        when(flowRunRepository.findRunForUpdate("101")).thenReturn(Optional.of(run));

        publisher.publish(event);

        assertEquals(FlowRunStatus.SUCCESS.getCode(), run.getStatus());
        assertEquals("pi-1", run.getEngineInstanceId());
        assertEquals(output, run.getOutputSummary());
        assertNotNull(run.getFinishedAt());
        assertTrue(run.getElapsedMs() >= 0);
        ArgumentCaptor<FlowRunEvent> eventCaptor = ArgumentCaptor.forClass(FlowRunEvent.class);
        verify(flowRunRepository).saveEvent(eventCaptor.capture());
        assertEquals("PROCESS_SUCCESS", eventCaptor.getValue().getEventType());
        assertEquals("__process__", eventCaptor.getValue().getNodeId());
        assertEquals("501", eventCaptor.getValue().getOutputPayloadRef());
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void lateCompletionCannotOverwriteTerminalFailure() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(FlowableVariableCodec.RUN_ID, "101");
        variables.put(FlowableVariableCodec.TENANT_ID, "7");
        FlowRun run = new FlowRun();
        run.setId("101");
        run.setStatus(FlowRunStatus.FAILED.getCode());
        when(event.getExecution()).thenReturn(execution);
        when(execution.getVariables()).thenReturn(variables);
        when(flowRunRepository.findRunForUpdate("101")).thenReturn(Optional.of(run));

        publisher.publish(event);

        assertEquals(FlowRunStatus.FAILED.getCode(), run.getStatus());
        verify(flowRunRepository, never()).saveRun(any());
        verify(flowRunRepository, never()).saveEvent(any());
    }

    @Test
    void deadLetterFailureKeepsTheJobEventInReplay() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(FlowableVariableCodec.RUN_ID, "101");
        variables.put(FlowableVariableCodec.TENANT_ID, "7");
        FlowRun run = new FlowRun();
        run.setId("101");
        run.setStatus(FlowRunStatus.RUNNING.getCode());
        run.setStartedAt(Instant.now().minusSeconds(2));
        when(jobEvent.getType()).thenReturn(FlowableEngineEventType.JOB_MOVED_TO_DEADLETTER);
        when(jobEvent.getProcessInstanceId()).thenReturn("pi-1");
        when(jobEvent.getProcessDefinitionId()).thenReturn("pd-1");
        when(jobEvent.getExecutionId()).thenReturn("ex-1");
        when(jobEvent.getEntity()).thenReturn(job);
        when(job.getElementId()).thenReturn("node_http");
        when(job.getExceptionMessage()).thenReturn("remote endpoint unavailable");
        when(runtimeService.getVariables("pi-1")).thenReturn(variables);
        when(exceptionTranslator.translateCode(any())).thenReturn("FLOWABLE_JOB_FAILED");
        when(flowRunRepository.findRunForUpdate("101")).thenReturn(Optional.of(run));

        publisher.publish(jobEvent);

        assertEquals(FlowRunStatus.FAILED.getCode(), run.getStatus());
        ArgumentCaptor<FlowRunEvent> eventCaptor = ArgumentCaptor.forClass(FlowRunEvent.class);
        verify(flowRunRepository, times(2)).saveEvent(eventCaptor.capture());
        assertEquals("JOB_MOVED_TO_DEADLETTER", eventCaptor.getAllValues().get(0).getEventType());
        assertEquals("http", eventCaptor.getAllValues().get(0).getNodeId());
        assertEquals("PROCESS_FAILED", eventCaptor.getAllValues().get(1).getEventType());
    }

    @Test
    void jobFailureCapturesTheCauseAndPreFailureRetryCount() {
        FlowableEngineEntityEvent failureEvent = mock(FlowableEngineEntityEvent.class,
                withSettings().extraInterfaces(FlowableExceptionEvent.class));
        RuntimeException cause = new RuntimeException("socket timeout");
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(FlowableVariableCodec.RUN_ID, "101");
        variables.put(FlowableVariableCodec.TENANT_ID, "7");
        FlowRun run = new FlowRun();
        run.setId("101");
        run.setStatus(FlowRunStatus.RUNNING.getCode());
        when(failureEvent.getType()).thenReturn(FlowableEngineEventType.JOB_EXECUTION_FAILURE);
        when(failureEvent.getProcessInstanceId()).thenReturn("pi-1");
        when(failureEvent.getProcessDefinitionId()).thenReturn("pd-1");
        when(failureEvent.getExecutionId()).thenReturn("ex-1");
        when(failureEvent.getEntity()).thenReturn(job);
        when(((FlowableExceptionEvent) failureEvent).getCause()).thenReturn(cause);
        when(job.getTenantId()).thenReturn("7");
        when(job.getElementId()).thenReturn("node_http");
        when(job.getRetries()).thenReturn(2);
        when(runtimeService.getVariables("pi-1")).thenReturn(variables);
        when(flowRunRepository.findRunByEngineInstanceId("pi-1")).thenReturn(Optional.of(run));
        when(flowRunRepository.findRunForUpdate("101")).thenReturn(Optional.of(run));
        when(exceptionTranslator.translateCode(cause)).thenReturn("FLOWABLE_JOB_FAILED");

        publisher.publish(failureEvent);

        ArgumentCaptor<FlowRunEvent> eventCaptor = ArgumentCaptor.forClass(FlowRunEvent.class);
        verify(flowRunRepository).saveEvent(eventCaptor.capture());
        assertEquals("JOB_EXECUTION_FAILURE", eventCaptor.getValue().getEventType());
        assertEquals("FLOWABLE_JOB_FAILED", eventCaptor.getValue().getErrorCode());
        assertEquals("socket timeout", eventCaptor.getValue().getErrorMessage());
        assertEquals(2, eventCaptor.getValue().getEventSummary().get("retriesBeforeFailure"));
    }

    @Test
    void userTaskCreationMovesTheRunToWaiting() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(FlowableVariableCodec.RUN_ID, "101");
        variables.put(FlowableVariableCodec.TENANT_ID, "7");
        FlowRun run = new FlowRun();
        run.setId("101");
        run.setStatus(FlowRunStatus.RUNNING.getCode());
        when(jobEvent.getType()).thenReturn(FlowableEngineEventType.TASK_CREATED);
        when(jobEvent.getProcessInstanceId()).thenReturn("pi-1");
        when(jobEvent.getProcessDefinitionId()).thenReturn("pd-1");
        when(jobEvent.getExecutionId()).thenReturn("ex-1");
        when(jobEvent.getEntity()).thenReturn(task);
        when(task.getTenantId()).thenReturn("7");
        when(task.getTaskDefinitionKey()).thenReturn("node_approval");
        when(task.getId()).thenReturn("task-1");
        when(runtimeService.getVariables("pi-1")).thenReturn(variables);
        when(flowRunRepository.findRunByEngineInstanceId("pi-1")).thenReturn(Optional.of(run));
        when(flowRunRepository.findRunForUpdate("101")).thenReturn(Optional.of(run));

        publisher.publish(jobEvent);

        assertEquals(FlowRunStatus.WAITING.getCode(), run.getStatus());
        ArgumentCaptor<FlowRunEvent> eventCaptor = ArgumentCaptor.forClass(FlowRunEvent.class);
        verify(flowRunRepository).saveEvent(eventCaptor.capture());
        assertEquals("TASK_CREATED", eventCaptor.getValue().getEventType());
        assertEquals("approval", eventCaptor.getValue().getNodeId());
        assertEquals("task-1", eventCaptor.getValue().getTaskId());
    }

    @Test
    void nativeTenantMustMatchTheRunVariables() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(FlowableVariableCodec.RUN_ID, "101");
        variables.put(FlowableVariableCodec.TENANT_ID, "8");
        when(event.getExecution()).thenReturn(execution);
        when(execution.getVariables()).thenReturn(variables);
        when(execution.getTenantId()).thenReturn("7");

        assertThrows(IllegalStateException.class, () -> publisher.publish(event));
        verifyNoInteractions(flowRunRepository);
    }

    @Test
    void unrelatedTenantAwareFlowableEventsAreIgnored() {
        when(jobEvent.getProcessInstanceId()).thenReturn("foreign-pi");
        when(jobEvent.getEntity()).thenReturn(task);
        when(task.getTenantId()).thenReturn("7");
        when(runtimeService.getVariables("foreign-pi")).thenReturn(new LinkedHashMap<>());

        assertDoesNotThrow(() -> publisher.publish(jobEvent));

        verify(flowRunRepository).findRunByEngineInstanceId("foreign-pi");
        verifyNoMoreInteractions(flowRunRepository);
    }
}
