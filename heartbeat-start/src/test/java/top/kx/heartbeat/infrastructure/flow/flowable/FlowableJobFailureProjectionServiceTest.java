package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlowableJobFailureProjectionServiceTest {
    @Mock
    private FlowProjectionPublisher projectionPublisher;
    @Mock
    private FlowableEvent event;
    @InjectMocks
    private FlowableJobFailureProjectionService service;

    @Test
    void delegatesTheRolledBackJobEventToTheRegularProjector() {
        service.publish(event);

        verify(projectionPublisher).publish(event);
    }
}
