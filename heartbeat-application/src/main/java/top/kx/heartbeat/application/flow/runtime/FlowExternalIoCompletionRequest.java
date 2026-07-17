package top.kx.heartbeat.application.flow.runtime;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result reported by a real external I/O worker.
 */
@Data
public class FlowExternalIoCompletionRequest {

    private String workerId;

    private String leaseToken;

    private String outcomeStatus;

    private Map<String, Object> output = new LinkedHashMap<>();

    private String errorCode;

    private String errorMessage;

    private int retryDelaySeconds = 5;
}
