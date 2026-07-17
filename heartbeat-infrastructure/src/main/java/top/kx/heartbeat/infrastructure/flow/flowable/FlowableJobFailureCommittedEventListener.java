package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class FlowableJobFailureCommittedEventListener implements FlowableEventListener {
    @Resource
    private FlowableJobFailureProjectionService projectionService;

    @Override
    public void onEvent(FlowableEvent event) {
        projectionService.publish(event);
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return true;
    }

    @Override
    public String getOnTransaction() {
        return TransactionState.COMMITTED.name();
    }
}
