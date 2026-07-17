package top.kx.heartbeat.application.flow.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class FlowOperationsSummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private long totalFlows;
    private long activeFlows;
    private long publishedFlows;
    private long draftFlows;
    private long totalRuns;
    private long runningRuns;
    private long waitingRuns;
    private long successRuns;
    private long failedRuns;
    private long canceledRuns;
    private long averageDurationMs;
    private BigDecimal successRate;
    private Instant startedAfter;
    private Instant startedBefore;
}
