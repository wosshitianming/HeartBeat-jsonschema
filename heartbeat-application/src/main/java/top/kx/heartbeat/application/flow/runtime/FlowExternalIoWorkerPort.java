package top.kx.heartbeat.application.flow.runtime;

import java.util.Optional;

/**
 * Reliable claim/start/complete contract implemented by the Flowable adapter.
 */
public interface FlowExternalIoWorkerPort {

    Optional<FlowExternalIoCommandView> claim(FlowExternalIoClaimRequest request);

    FlowExternalIoCommandView markCallStarted(String commandId, FlowExternalIoStartedRequest request);

    FlowExternalIoCommandView renewLease(String commandId, FlowExternalIoStartedRequest request);

    FlowExternalIoCommandView complete(String commandId, FlowExternalIoCompletionRequest request);

    FlowExternalIoCommandView resolve(String commandId, FlowExternalIoResolutionRequest request);
}
