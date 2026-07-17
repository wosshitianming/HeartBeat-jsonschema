package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class FlowableJobFailureProjectionService {
    @Resource
    private FlowProjectionPublisher projectionPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(FlowableEvent event) {
        projectionPublisher.publish(event);
    }
}
