package top.kx.heartbeat.domain.tool;

import top.kx.heartbeat.domain.tool.model.JobExecutionLog;
import top.kx.heartbeat.domain.tool.model.ScheduledJob;

import java.util.List;
import java.util.Optional;

public interface ScheduledJobRepository {

    List<ScheduledJob> findAll();

    Optional<ScheduledJob> findByCode(String jobCode);

    void appendExecutionLog(JobExecutionLog log);
}
