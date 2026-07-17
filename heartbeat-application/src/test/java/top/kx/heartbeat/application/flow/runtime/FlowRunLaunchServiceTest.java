package top.kx.heartbeat.application.flow.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunStatus;
import top.kx.heartbeat.domain.flow.model.FlowRuntimeEngine;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowRunLaunchServiceTest {

    @Mock
    private FlowRuntimeFacade flowRuntimeFacade;
    @Mock
    private FlowRunRepository flowRunRepository;
    @Mock
    private FlowRunLedgerService ledgerService;
    @Mock
    private FlowPayloadSummaryService payloadSummaryService;
    @InjectMocks
    private FlowRunLaunchService service;

    @Test
    void returnsTerminalProjectionWithoutOverwritingItWithRunningState() {
        FlowStartCommand command = new FlowStartCommand();
        command.setRunId("101");
        FlowRun pending = new FlowRun();
        pending.setId("101");
        pending.setStatus(FlowRunStatus.CREATED.getCode());
        FlowRun started = new FlowRun();
        started.setId("101");
        started.setStatus(FlowRunStatus.RUNNING.getCode());
        FlowRun projected = new FlowRun();
        projected.setId("101");
        projected.setStatus(FlowRunStatus.SUCCESS.getCode());
        when(flowRuntimeFacade.productionEngine()).thenReturn(FlowRuntimeEngine.FLOWABLE);
        when(flowRuntimeFacade.start(command)).thenReturn(started);
        when(flowRunRepository.findRun("101")).thenReturn(Optional.of(projected));

        FlowRun result = service.start(command, pending);

        assertSame(projected, result);
        verify(ledgerService).create(pending);
        verify(flowRunRepository, never()).saveRun(started);
    }

    @Test
    void recordsSynchronousStartFailureInIndependentLedgerTransaction() {
        FlowStartCommand command = new FlowStartCommand();
        command.setRunId("101");
        FlowRun pending = new FlowRun();
        pending.setId("101");
        RuntimeException failure = new RuntimeException("boom");
        when(flowRuntimeFacade.productionEngine()).thenReturn(FlowRuntimeEngine.FLOWABLE);
        when(flowRuntimeFacade.start(command)).thenThrow(failure);

        assertThrows(RuntimeException.class, () -> service.start(command, pending));
        verify(ledgerService).create(pending);
        verify(ledgerService).markStartFailed(pending, failure);
    }

    @Test
    void pendingRunStoresOnlyThePayloadSummary() {
        FlowStartCommand command = new FlowStartCommand();
        command.setRunId("101");
        command.setPayload(Collections.singletonMap("large", "payload"));
        when(flowRuntimeFacade.productionEngine()).thenReturn(FlowRuntimeEngine.FLOWABLE);
        when(payloadSummaryService.summarize(command.getPayload()))
                .thenReturn(Collections.singletonMap("payloadSummary", "HB_FLOW_PAYLOAD_SUMMARY_V1"));

        FlowRun pending = service.createPending(command);

        assertEquals("HB_FLOW_PAYLOAD_SUMMARY_V1", pending.getInputSummary().get("payloadSummary"));
        verify(payloadSummaryService).summarize(command.getPayload());
    }
}
