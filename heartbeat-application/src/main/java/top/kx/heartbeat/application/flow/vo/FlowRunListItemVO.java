package top.kx.heartbeat.application.flow.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class FlowRunListItemVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String flowId;
    private String flowName;
    private int versionNo;
    private String runNo;
    private String engine;
    private String triggerType;
    private String status;
    private String retryFromRunId;
    private Integer retryNo;
    private Instant startedAt;
    private Instant finishedAt;
    private Long elapsedMs;
    private boolean retryable;
}
