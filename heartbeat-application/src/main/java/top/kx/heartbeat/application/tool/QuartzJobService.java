package top.kx.heartbeat.application.tool;


import org.springframework.stereotype.Service;
import top.kx.heartbeat.domain.tool.QuartzJobScheduler;
import top.kx.heartbeat.domain.tool.ScheduledJobRepository;
import top.kx.heartbeat.domain.tool.model.ScheduledJob;

import javax.annotation.Resource;

/**
 * 定时任务应用服务：从专用 sys_job 表同步 Quartz 并提供运行控制。
 */
@Service
public class QuartzJobService {

    @Resource
    private ScheduledJobRepository scheduledJobRepository;
    @Resource
    private QuartzJobScheduler quartzJobScheduler;

    public void refreshScheduler() {
        quartzJobScheduler.refreshJobs(scheduledJobRepository.findAll());
    }

    public void runNow(String jobCode) {
        ScheduledJob job = findJobRequired(jobCode);
        quartzJobScheduler.runNow(job.getJobCode(), job.getJobGroup());
    }

    public void pause(String jobCode) {
        ScheduledJob job = findJobRequired(jobCode);
        quartzJobScheduler.pause(job.getJobCode(), job.getJobGroup());
    }

    public void resume(String jobCode) {
        ScheduledJob job = findJobRequired(jobCode);
        quartzJobScheduler.resume(job.getJobCode(), job.getJobGroup());
    }

    private ScheduledJob findJobRequired(String jobCode) {
        return scheduledJobRepository.findByCode(jobCode)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + jobCode));
    }
}
