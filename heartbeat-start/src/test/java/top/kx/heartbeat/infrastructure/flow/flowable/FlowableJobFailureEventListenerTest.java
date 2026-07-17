package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlowableJobFailureEventListenerTest {
    @Mock
    private FlowableJobFailureProjectionService projectionService;
    @Mock
    private FlowableEvent event;
    @InjectMocks
    private FlowableJobFailureEventListener listener;

    @Test
    void jobFailureProjectionRunsAfterTheEngineTransactionRollsBack() {
        assertTrue(listener.isFireOnTransactionLifecycleEvent());
        assertFalse(listener.isFailOnException());
        assertEquals(TransactionState.ROLLED_BACK.name(), listener.getOnTransaction());

        listener.onEvent(event);

        verify(projectionService).publish(event);
    }
}
