package top.kx.heartbeat.domain.tool.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class JobExecutionLog {
    Long tenantId;
    Long jobId;
    String jobCode;
    String invokeTarget;
    String resultStatus;
    String message;
    long durationMs;
    Instant startedAt;
    Instant finishedAt;
}
