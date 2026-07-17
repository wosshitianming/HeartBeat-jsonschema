package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FlowableRuntimeConfigurationTest {
    @Test
    void jobFailuresUseTheRollbackListenerWhileRuntimeEventsStayImmediate() {
        FlowableProjectionEventListener runtimeListener = mock(FlowableProjectionEventListener.class);
        FlowableJobFailureCommittedEventListener committedJobFailureListener =
                mock(FlowableJobFailureCommittedEventListener.class);
        FlowableJobFailureEventListener jobFailureListener = mock(FlowableJobFailureEventListener.class);
        SpringProcessEngineConfiguration engine = mock(SpringProcessEngineConfiguration.class);
        EngineConfigurationConfigurer<SpringProcessEngineConfiguration> configurer =
                new FlowableRuntimeConfiguration().flowProjectionConfigurer(
                        runtimeListener, committedJobFailureListener, jobFailureListener);

        configurer.configure(engine);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<FlowableEventListener>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(engine).setTypedEventListeners(captor.capture());
        Map<String, List<FlowableEventListener>> listeners = captor.getValue();
        assertEquals(java.util.Arrays.asList(committedJobFailureListener, jobFailureListener),
                listeners.get(FlowableEngineEventType.JOB_EXECUTION_FAILURE.name()));
        assertTrue(listeners.get(FlowableEngineEventType.PROCESS_STARTED.name()).contains(runtimeListener));
        assertTrue(listeners.get(FlowableEngineEventType.TASK_CREATED.name()).contains(runtimeListener));
        assertFalse(listeners.get(FlowableEngineEventType.PROCESS_STARTED.name()).contains(jobFailureListener));
    }
}
