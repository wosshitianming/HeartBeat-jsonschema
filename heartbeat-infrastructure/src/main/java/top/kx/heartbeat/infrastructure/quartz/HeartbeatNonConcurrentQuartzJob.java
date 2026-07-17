package top.kx.heartbeat.infrastructure.quartz;

import org.quartz.DisallowConcurrentExecution;

/**
 * Quartz entry used when a sys_job row explicitly forbids overlapping executions.
 */
@DisallowConcurrentExecution
public class HeartbeatNonConcurrentQuartzJob extends HeartbeatQuartzJob {
}
