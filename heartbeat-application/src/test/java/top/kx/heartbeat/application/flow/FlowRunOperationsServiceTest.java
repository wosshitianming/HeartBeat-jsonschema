package top.kx.heartbeat.application.flow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.kx.heartbeat.application.common.vo.PageResultVO;
import top.kx.heartbeat.application.flow.param.FlowRunQueryParam;
import top.kx.heartbeat.application.flow.param.FlowRunSummaryParam;
import top.kx.heartbeat.application.flow.runtime.FlowRunIdGenerator;
import top.kx.heartbeat.application.flow.runtime.FlowRunLaunchService;
import top.kx.heartbeat.application.flow.runtime.FlowStartCommand;
import top.kx.heartbeat.application.flow.vo.FlowOperationsSummaryVO;
import top.kx.heartbeat.application.flow.vo.FlowRunListItemVO;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;
import top.kx.heartbeat.domain.flow.model.*;
import top.kx.heartbeat.domain.flow.repository.FlowRepository;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowRunOperationsServiceTest {

    @Mock
    private FlowRunRepository flowRunRepository;
    @Mock
    private FlowRepository flowRepository;
    @Mock
    private FlowRunLaunchService flowRunLaunchService;
    @Mock
    private FlowRunIdGenerator runIdGenerator;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @InjectMocks
    private FlowRunOperationsService service;

    @Test
    void pageNormalizesFiltersAndReturnsLightweightRows() {
        FlowRun run = new FlowRun();
        run.setId("101");
        run.setFlowId("11");
        run.setStatus(FlowRunStatus.FAILED.getCode());
        run.setTriggerType(FlowTriggerType.MANUAL.getCode());
        run.setStartedAt(Instant.parse("2026-07-11T01:00:00Z"));
        FlowDefinition flow = new FlowDefinition();
        flow.setId("11");
        flow.setName("订单同步");
        when(flowRunRepository.pageByQuery(any())).thenReturn(
                new FlowRunRepository.Page<>(Collections.singletonList(run), 1L, 1, 20));
        when(flowRepository.findAll()).thenReturn(Collections.singletonList(flow));

        FlowRunQueryParam param = new FlowRunQueryParam();
        param.setStatuses(Collections.singletonList("failed"));
        param.setTriggerTypes(Collections.singletonList("manual"));
        PageResultVO<FlowRunListItemVO> page = service.page(param);

        assertEquals(1L, page.getTotal());
        assertEquals("订单同步", page.getRecords().get(0).getFlowName());
        assertTrue(page.getRecords().get(0).isRetryable());
        ArgumentCaptor<FlowRunQuery> captor = ArgumentCaptor.forClass(FlowRunQuery.class);
        verify(flowRunRepository).pageByQuery(captor.capture());
        assertEquals(Collections.singletonList("FAILED"), captor.getValue().getStatuses());
        assertEquals(Collections.singletonList("MANUAL"), captor.getValue().getTriggerTypes());
    }

    @Test
    void summaryCalculatesSuccessRateForDefaultWindow() {
        FlowDefinition online = new FlowDefinition();
        online.setStatus(FlowDefinitionStatus.ONLINE.getCode());
        online.setActiveVersionNo(2);
        FlowDefinition draft = new FlowDefinition();
        draft.setStatus(FlowDefinitionStatus.DRAFT.getCode());
        FlowRunStatistics stats = new FlowRunStatistics();
        stats.setTotalRuns(12);
        stats.setSuccessRuns(8);
        stats.setFailedRuns(2);
        stats.setRunningRuns(1);
        stats.setWaitingRuns(1);
        stats.setAverageDurationMs(350);
        when(flowRepository.findAll()).thenReturn(Arrays.asList(online, draft));
        when(flowRunRepository.summarize(any(), any(), any())).thenReturn(stats);

        FlowOperationsSummaryVO summary = service.summary(new FlowRunSummaryParam());

        assertEquals(2, summary.getTotalFlows());
        assertEquals(1, summary.getActiveFlows());
        assertEquals(new BigDecimal("80.00"), summary.getSuccessRate());
        assertTrue(summary.getStartedAfter().isBefore(summary.getStartedBefore()));
    }

    @Test
    void retryUsesOriginalVersionAndRecordsLineage() {
        FlowRun original = new FlowRun();
        original.setId("101");
        original.setFlowId("11");
        original.setVersionNo(3);
        original.setStatus(FlowRunStatus.FAILED.getCode());
        original.setInputSummary(new LinkedHashMap<>(Collections.singletonMap("orderId", 7)));
        FlowDefinition flow = new FlowDefinition();
        flow.setId("11");
        flow.setCode("order_sync");
        FlowVersion version = new FlowVersion();
        version.setId("33");
        version.setVersionNo(3);
        version.setProcessDefinitionId("process:3:99");
        version.setProcessDefinitionKey("order_sync");
        when(flowRunRepository.findRun("101")).thenReturn(Optional.of(original));
        when(flowRepository.findById("11")).thenReturn(Optional.of(flow));
        when(flowRepository.findVersion("11", 3)).thenReturn(Optional.of(version));
        when(runIdGenerator.nextId()).thenReturn("202");
        when(currentUserProvider.currentTenantId()).thenReturn("7");
        when(flowRunLaunchService.createPending(any())).thenAnswer(invocation -> {
            FlowStartCommand command = invocation.getArgument(0);
            FlowRun pending = new FlowRun();
            pending.setId(command.getRunId());
            pending.setStatus(FlowRunStatus.CREATED.getCode());
            return pending;
        });
        when(flowRunLaunchService.start(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        FlowRun retried = service.retry("101", null);

        assertEquals("202", retried.getId());
        assertEquals("101", retried.getRetryFromRunId());
        assertEquals("101", retried.getRootRunId());
        assertEquals(1, retried.getRetryNo());
        assertEquals(FlowIdempotencyScope.USER_RETRY.getCode(), retried.getIdempotencyScope());
        ArgumentCaptor<FlowStartCommand> commandCaptor = ArgumentCaptor.forClass(FlowStartCommand.class);
        verify(flowRunLaunchService).createPending(commandCaptor.capture());
        assertEquals(3, commandCaptor.getValue().getVersionNo());
        assertEquals("process:3:99", commandCaptor.getValue().getProcessDefinitionId());
        assertEquals(FlowTriggerType.RETRY, commandCaptor.getValue().getTriggerType());
    }

    @Test
    void retryRequiresExplicitVariablesWhenTheLedgerOnlyHasAPayloadReference() {
        FlowRun original = new FlowRun();
        original.setId("101");
        original.setFlowId("11");
        original.setVersionNo(3);
        original.setStatus(FlowRunStatus.FAILED.getCode());
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("payloadStorage", "HB_FLOW_PAYLOAD_AES_GCM_V1");
        summary.put("payloadRef", "501");
        original.setInputSummary(summary);
        FlowDefinition flow = new FlowDefinition();
        flow.setId("11");
        FlowVersion version = new FlowVersion();
        version.setVersionNo(3);
        when(flowRunRepository.findRun("101")).thenReturn(Optional.of(original));
        when(flowRepository.findById("11")).thenReturn(Optional.of(flow));
        when(flowRepository.findVersion("11", 3)).thenReturn(Optional.of(version));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> service.retry("101", null));

        assertTrue(error.getMessage().contains("必须显式提供 variables"));
        verifyNoInteractions(flowRunLaunchService);
    }
}
