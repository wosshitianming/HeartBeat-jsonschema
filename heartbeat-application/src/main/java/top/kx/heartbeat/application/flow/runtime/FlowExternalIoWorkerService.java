package top.kx.heartbeat.application.flow.runtime;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * Application entry point for independently deployed external I/O workers.
 */
@Service
public class FlowExternalIoWorkerService {

    @Resource
    private FlowExternalIoWorkerPort workerPort;

    public Optional<FlowExternalIoCommandView> claim(FlowExternalIoClaimRequest request) {
        return workerPort.claim(request);
    }

    public FlowExternalIoCommandView markCallStarted(String commandId, FlowExternalIoStartedRequest request) {
        return workerPort.markCallStarted(commandId, request);
    }

    public FlowExternalIoCommandView renewLease(String commandId, FlowExternalIoStartedRequest request) {
        return workerPort.renewLease(commandId, request);
    }

    public FlowExternalIoCommandView complete(String commandId, FlowExternalIoCompletionRequest request) {
        return workerPort.complete(commandId, request);
    }

    public FlowExternalIoCommandView resolve(String commandId, FlowExternalIoResolutionRequest request) {
        return workerPort.resolve(commandId, request);
    }
}
