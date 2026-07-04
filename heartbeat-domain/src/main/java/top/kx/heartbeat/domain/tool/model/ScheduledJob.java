package top.kx.heartbeat.domain.tool.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ScheduledJob {
    Long id;
    Long tenantId;
    String jobCode;
    String jobName;
    String jobGroup;
    String invokeTarget;
    String cronExpression;
    String misfirePolicy;
    boolean concurrent;
    String status;
}
