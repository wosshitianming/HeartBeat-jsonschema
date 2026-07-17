package top.kx.heartbeat.application.flow.runtime;

import lombok.Data;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tenant-scoped command handed to an external I/O worker.
 */
@Data
public class FlowExternalIoCommandView {

    private String commandId;

    private String runId;

    private String nodeId;

    private String nodeType;

    private String nodeVersion;

    private String executorId;

    private String workerTopic;

    private String idempotencyKey;

    private String status;

    private int attemptNo;

    private int maxAttempts;

    private Instant leaseUntil;

    private String leaseToken;

    private long leaseVersion;

    private Instant timeoutAt;

    private Map<String, Object> request = new LinkedHashMap<>();
}
