package top.kx.heartbeat.application.flow.param;

import lombok.Data;

import java.time.Instant;

@Data
public class FlowRunSummaryParam {
    private String flowId;
    private Instant startedAfter;
    private Instant startedBefore;
}
