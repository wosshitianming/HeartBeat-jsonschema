package top.kx.heartbeat.application.flow.param;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class FlowRunQueryParam {
    private Integer pageNum;
    private Integer pageSize;
    private String flowId;
    private List<String> statuses = new ArrayList<>();
    private List<String> triggerTypes = new ArrayList<>();
    private Instant startedAfter;
    private Instant startedBefore;
    private String orderByColumn;
    private String orderByDirection;
}
