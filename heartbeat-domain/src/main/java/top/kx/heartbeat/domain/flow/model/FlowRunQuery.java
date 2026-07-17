package top.kx.heartbeat.domain.flow.model;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class FlowRunQuery {
    private String flowId;
    private List<String> statuses = new ArrayList<>();
    private List<String> triggerTypes = new ArrayList<>();
    private Instant startedAfter;
    private Instant startedBefore;
    private String orderByColumn;
    private String orderByDirection;
    private int pageNum = 1;
    private int pageSize = 20;
}
