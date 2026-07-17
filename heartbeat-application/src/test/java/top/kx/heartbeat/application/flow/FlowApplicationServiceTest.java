package top.kx.heartbeat.application.flow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.kx.heartbeat.application.flow.runtime.*;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.model.FlowDefinitionStatus;
import top.kx.heartbeat.domain.flow.model.FlowRuntimeEngine;
import top.kx.heartbeat.domain.flow.model.FlowVersion;
import top.kx.heartbeat.domain.flow.repository.FlowRepository;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;
import top.kx.heartbeat.domain.flow.validation.FlowDslValidator;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowApplicationServiceTest {

    @Mock
    private FlowRepository flowRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private NodeComponentRegistryService componentRegistryService;
    @Mock
    private FlowExecutor flowExecutor;
    @Mock
    private FlowBpmnCompiler flowBpmnCompiler;
    @Mock
    private FlowRuntimeFacade flowRuntimeFacade;
    @Mock
    private FlowRunRepository flowRunRepository;
    @Mock
    private FlowRunIdGenerator flowRunIdGenerator;
    @Mock
    private FlowRunLaunchService flowRunLaunchService;
    @Mock
    private FlowDslValidator validator;
    @InjectMocks
    private FlowApplicationService service;

    @Test
    void activateSynchronizesVersionAndRuntimeDeployment() {
        FlowVersion version = new FlowVersion();
        version.setVersionNo(2);
        version.setCompileStatus("DEPLOYED");
        version.setRuntimeEngine("FLOWABLE");
        version.setDeploymentId("deployment-2");
        version.setProcessDefinitionId("definition-2");
        FlowDefinition activated = new FlowDefinition();
        activated.setId("11");
        activated.setActiveVersionNo(2);
        when(flowRepository.findVersion("11", 2)).thenReturn(Optional.of(version));
        when(flowRepository.findById("11")).thenReturn(Optional.of(activated));
        when(flowRuntimeFacade.productionEngine()).thenReturn(FlowRuntimeEngine.FLOWABLE);

        FlowDefinition result = service.activate("11", 2);

        assertSame(activated, result);
        verify(flowRepository).activateVersion("11", 2);
        verify(flowRepository).updateActiveRuntimeDeployment(
                "11", "FLOWABLE", "deployment-2", "definition-2");
    }

    @Test
    void savingDraftPreservesOnlineAndServerOwnedRuntimeFields() {
        Instant createdAt = Instant.parse("2026-07-01T00:00:00Z");
        FlowDefinition existing = new FlowDefinition();
        existing.setId("11");
        existing.setStatus(FlowDefinitionStatus.ONLINE.getCode());
        existing.setActiveVersionNo(2);
        existing.setRuntimeEngine("FLOWABLE");
        existing.setActiveDeploymentId("deployment-2");
        existing.setActiveProcessDefinitionId("definition-2");
        existing.setCreateTime(createdAt);
        FlowDefinition incoming = new FlowDefinition();
        incoming.setId("11");
        incoming.setName("更新后的名称");
        incoming.setStatus(FlowDefinitionStatus.DRAFT.getCode());
        incoming.setActiveVersionNo(99);
        when(flowRepository.findById("11")).thenReturn(Optional.of(existing));
        when(flowRepository.saveDraft(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FlowDefinition saved = service.saveDraft(incoming);

        assertEquals(FlowDefinitionStatus.ONLINE.getCode(), saved.getStatus());
        assertEquals(2, saved.getActiveVersionNo());
        assertEquals("deployment-2", saved.getActiveDeploymentId());
        assertEquals("definition-2", saved.getActiveProcessDefinitionId());
        assertEquals(createdAt, saved.getCreateTime());
    }
}
