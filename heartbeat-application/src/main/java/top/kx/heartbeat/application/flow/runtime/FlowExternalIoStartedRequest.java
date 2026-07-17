package top.kx.heartbeat.application.flow.runtime;

import lombok.Data;

/**
 * Worker acknowledgement sent immediately before an external side effect starts.
 */
@Data
public class FlowExternalIoStartedRequest {

    private String workerId;

    private String leaseToken;

    private int leaseSeconds = 60;
}
