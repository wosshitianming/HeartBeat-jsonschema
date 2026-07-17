package top.kx.heartbeat.domain.flow.model;

import lombok.Data;

@Data
public class FlowRunStatistics {
    private long totalRuns;
    private long runningRuns;
    private long successRuns;
    private long failedRuns;
    private long canceledRuns;
    private long waitingRuns;
    private long averageDurationMs;
}
